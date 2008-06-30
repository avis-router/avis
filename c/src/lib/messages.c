/*
 *  Avis Elvin client library for C.
 *
 *  Copyright (C) 2008 Matthew Phillips <avis@mattp.name>
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of version 3 of the GNU Lesser General
 *  Public License as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <assert.h>

#include <avis/stdtypes.h>
#include <avis/errors.h>
#include <avis/attributes.h>
#include <avis/values.h>

#include "messages.h"
#include "byte_buffer.h"
#include "attributes_private.h"
#include "values_private.h"
#include "keys_private.h"
#include "log.h"

static void read_int32 (ByteBuffer *buffer, Message message,
                        ElvinError *error);

static void write_int32 (ByteBuffer *buffer, Message message,
                         ElvinError *error);

static void read_int64 (ByteBuffer *buffer, Message message,
                        ElvinError *error);

static void write_int64 (ByteBuffer *buffer, Message message,
                         ElvinError *error);

static void read_int64_array (ByteBuffer *buffer, Message message,
                              ElvinError *error);

static void write_int64_array (ByteBuffer *buffer, Message message,
                               ElvinError *error);

static void read_bool (ByteBuffer *buffer, Message message,
                       ElvinError *error);

static void read_xid (ByteBuffer *buffer, Message message,
                      ElvinError *error);

static void read_string (ByteBuffer *buffer, Message message,
                         ElvinError *error);

static void write_string (ByteBuffer *buffer, Message message,
                          ElvinError *error);

static void read_attributes (ByteBuffer *buffer, Message message,
                             ElvinError *error);

static void write_attributes (ByteBuffer *buffer, Message message,
                              ElvinError *error);

static void read_values (ByteBuffer *buffer, Message message,
                         ElvinError *error);

static void write_values (ByteBuffer *buffer, Message message,
                          ElvinError *error);

static void read_keys (ByteBuffer *buffer, Message message,
                       ElvinError *error);

static void write_keys (ByteBuffer *buffer, Message message,
                        ElvinError *error);

static void values_free (Array *values);

typedef enum
{
  FIELD_XID = 0, FIELD_INT32 = 1, FIELD_INT64 = 2, FIELD_POINTER = 3
} FieldType;

/** Size of FieldType values */
static int field_sizes [4] = {4, 4, 8, sizeof (void *)};

typedef void (*MessageIOFunction) (ByteBuffer *buffer, Message message,
                                   ElvinError *error);

typedef void (*DeallocFunction) ();

typedef struct
{
  FieldType type;
  MessageIOFunction read;
  MessageIOFunction write;
  DeallocFunction free;
} FieldFormat;

typedef struct
{
  MessageTypeID id;
  FieldFormat fields [MAX_MESSAGE_FIELDS];
} MessageFormat;

#define I32  {FIELD_INT32, read_int32, write_int32, NULL}
#define I64  {FIELD_INT64, read_int64, write_int64, NULL}
#define I64A {FIELD_POINTER, read_int64_array, write_int64_array, array_free}
#define STR  {FIELD_POINTER, read_string, write_string, NULL}
#define ATTR {FIELD_POINTER, read_attributes, write_attributes, attributes_free}
#define BO   {FIELD_INT32, read_bool, write_int32, NULL}
#define XID  {FIELD_XID, read_xid, write_int32, NULL}
#define VA   {FIELD_POINTER, read_values, write_values, values_free}
#define KEYS {FIELD_POINTER, read_keys, write_keys, elvin_keys_free}

#define END {0, (MessageIOFunction)NULL, (MessageIOFunction)NULL, NULL}

static MessageFormat MESSAGE_FORMATS [] =
{
  {MESSAGE_ID_NACK,
    {XID, I32, STR, VA, END} /* {"xid", "error", "message", "args"}*/},
  {MESSAGE_ID_CONN_RQST,
    {XID, I32, I32, ATTR, KEYS, KEYS, END}
    /*{"xid", "version_major", "version_minor",
     "connection_options", "notification_keys" "subscription_keys"}*/},
  {MESSAGE_ID_CONN_RPLY,
    {XID, ATTR, END} /*{"xid", "connection_options"}*/},
  {MESSAGE_ID_DISCONN_RQST,
    {XID, END} /*{"xid"}*/},
  {MESSAGE_ID_DISCONN_RPLY,
    {XID, END} /*{"xid"}*/},
  {MESSAGE_ID_DISCONN,
    {I32, STR, END} /*{"reason", "arguments"}*/},
  {MESSAGE_ID_SEC_RQST,
    {XID, KEYS, KEYS, KEYS, KEYS, END}},
  {MESSAGE_ID_SEC_RPLY,
    {XID, END}},
  {MESSAGE_ID_NOTIFY_EMIT,
    {ATTR, BO, KEYS, END} /*{"attributes", "deliverInsecure", "keys"}*/},
  {MESSAGE_ID_NOTIFY_DELIVER,
    {ATTR, I64A, I64A, END}
    /* {"attributes", "secureMatches", "insecureMatches"}*/},
  {MESSAGE_ID_SUB_ADD_RQST,
    {XID, STR, BO, KEYS, END} /*{"xid", "expr", "deliverInsecure", "keys"}*/},
  {MESSAGE_ID_SUB_MOD_RQST,
    {XID, I64, STR, BO, KEYS, KEYS, END}},
  {MESSAGE_ID_SUB_DEL_RQST,
    {XID, I64, END} /*{"xid", "subscriptionId"}*/},
  {MESSAGE_ID_SUB_RPLY,
    {XID, I64, END} /*{"xid"}*/},

  {-1, {END}}
};

static MessageFormat *message_format_for (MessageTypeID type);

static void read_using_format (ByteBuffer *buffer,
                               Message message,
                               MessageFormat *format,
                               ElvinError *error);

static void write_using_format (ByteBuffer *buffer,
                                MessageFormat *format,
                                Message message,
                                ElvinError *error);

static uint32_t xid_counter = 1;

#define next_xid() (xid_counter++)

Message message_init (Message message, MessageTypeID type, ...)
{
  MessageFormat *format = message_format_for (type);
  va_list args;
  FieldFormat *field;

  assert (format != NULL);

  message_type_of (message) = type;

  message += 4;

  va_start (args, type);

  for (field = format->fields; field->read; field++)
  {
    switch (field->type)
    {
    case FIELD_XID:
      *(uint32_t *)message = next_xid ();
      message += 4;
      break;
    case FIELD_INT32:
      *(int32_t *)message = va_arg (args, int32_t);
      message += 4;
      break;
    case FIELD_INT64:
      *(int64_t *)message = va_arg (args, int64_t);
      message += 8;
      break;
    case FIELD_POINTER:
      *(void **)message = va_arg (args, void *);
      message += sizeof (void *);
      break;
    }
  }

  va_end (args);

  return message;
}

void message_free (Message message)
{
  MessageFormat *format = message_format_for (message_type_of (message));
  Message message_field = message + 4;
  FieldFormat *field;

  /* will happen on zeroed-out message block */
  if (format == NULL)
    return;

  for (field = format->fields; field->read; field++)
  {
    if (field->type == FIELD_POINTER)
    {
      void *ptr = *(void **)message_field;

      if (ptr)
      {
        if (field->free)
          (*field->free) (ptr);

        free (ptr);
      }
    }

    message_field += field_sizes [field->type];
  }

  memset (message, 0, MAX_MESSAGE_SIZE);
}

bool message_read (ByteBuffer *buffer, Message message, ElvinError *error)
{
  uint32_t type;
  MessageFormat *format;

  memset (message, 0, MAX_MESSAGE_SIZE);

  on_error_return_false (type = byte_buffer_read_int32 (buffer, error));

  format = message_format_for (type);

  if (format == NULL)
  {
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL,
                     "Unknown message type: %u", type);

    return false;
  }

  /* fill in type field */
  message_type_of (message) = type;

  read_using_format (buffer, message + 4, format, error);

  if (buffer->position < buffer->max_data_length &&
      elvin_error_ok (error))
  {
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Message underflow");
  }

  if (elvin_error_ok (error))
  {
    return true;
  } else
  {
    message_free (message);

    memset (message, 0, MAX_MESSAGE_SIZE);

    return false;
  }
}

bool message_write (ByteBuffer *buffer, Message message, ElvinError *error)
{
  size_t frame_size;
  MessageFormat *format = message_format_for (message_type_of (message));

  assert (format != NULL);

  on_error_return_false
    (byte_buffer_skip (buffer, 4, error));
  on_error_return_false
    (byte_buffer_write_int32 (buffer, message_type_of (message), error));

  write_using_format (buffer, format, message + 4, error);

  if (elvin_error_occurred (error))
    return false;

  frame_size = buffer->position - 4;

  /* write frame length */
  byte_buffer_set_position (buffer, 0, error);
  byte_buffer_write_int32 (buffer, (uint32_t)frame_size, error);

  byte_buffer_set_position (buffer, frame_size + 4, error);

  return true;
}

void read_using_format (ByteBuffer *buffer,
                        Message message,
                        MessageFormat *format,
                        ElvinError *error)
{
  FieldFormat *field;

  for (field = format->fields; field->read && elvin_error_ok (error); field++)
  {
    (*field->read) (buffer, message, error);

    message += field_sizes [field->type];
  }
}

void write_using_format (ByteBuffer *buffer,
                         MessageFormat *format,
                         Message message,
                         ElvinError *error)
{
  FieldFormat *field;

  for (field = format->fields; field->write && elvin_error_ok (error); field++)
  {
    (*field->write) (buffer, message, error);

    message += field_sizes [field->type];
  }
}

MessageFormat *message_format_for (MessageTypeID type)
{
  MessageFormat *format;

  for (format = MESSAGE_FORMATS; format->id != -1; format++)
  {
    if (format->id == type)
      return format;
  }

  DIAGNOSTIC1 ("Failed to lookup info for message type %i", type);

  return NULL;
}

void read_int32 (ByteBuffer *buffer, Message message, ElvinError *error)
{
  *(int32_t *)message = byte_buffer_read_int32 (buffer, error);
}

void write_int32 (ByteBuffer *buffer, Message message, ElvinError *error)
{
  byte_buffer_write_int32 (buffer, *(int32_t *)message, error);
}

void read_int64 (ByteBuffer *buffer, Message message, ElvinError *error)
{
  *(int64_t *)message = byte_buffer_read_int64 (buffer, error);
}

void write_int64 (ByteBuffer *buffer, Message message, ElvinError *error)
{
  byte_buffer_write_int64 (buffer, *(int64_t *)message, error);
}

void read_int64_array (ByteBuffer *buffer, Message message,
                       ElvinError *error)
{
  uint32_t item_count = byte_buffer_read_int32 (buffer, error);

  if (elvin_error_ok (error))
  {
    Array *array = array_create (int64_t, item_count);
    int64_t *item = (int64_t *)array->items;

    for ( ; item_count > 0 && elvin_error_ok (error); item_count--, item++)
      *item = byte_buffer_read_int64 (buffer, error);

    *(Array **)message = array;
  }
}

void write_int64_array (ByteBuffer *buffer, Message message,
                        ElvinError *error)
{
  size_t item_count = (*(Array **)message)->item_count;
  int64_t *item = (int64_t *)(*(Array **)message)->items;

  byte_buffer_write_int32 (buffer, item_count, error);

  for ( ; item_count > 0 && elvin_error_ok (error); item_count--, item++)
    byte_buffer_write_int64 (buffer, *item, error);
}

void read_bool (ByteBuffer *buffer, Message message, ElvinError *error)
{
  uint32_t value = byte_buffer_read_int32 (buffer, error);

  *(uint32_t *)message = value;

  if (value > 1 && elvin_error_ok (error))
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Invalid boolean: %u", value);
}

void read_xid (ByteBuffer *buffer, Message message, ElvinError *error)
{
  uint32_t value = byte_buffer_read_int32 (buffer, error);

  *(uint32_t *)message = value;

  if (value == 0 && elvin_error_ok (error))
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "XID cannot be zero", value);
}

void read_string (ByteBuffer *buffer, Message message, ElvinError *error)
{
  *(char **)message = byte_buffer_read_string (buffer, error);
}

void write_string (ByteBuffer *buffer, Message message, ElvinError *error)
{
  byte_buffer_write_string (buffer, *(char **)message, error);
}

void read_attributes (ByteBuffer *buffer, Message message,
                      ElvinError *error)
{
  Attributes *attributes = attributes_create ();

  attributes_read (buffer, attributes, error);

  *(Attributes **)message = attributes;
}

void write_attributes (ByteBuffer *buffer, Message message,
                            ElvinError *error)
{
  attributes_write (buffer, *(Attributes **)message, error);
}

void read_keys (ByteBuffer *buffer, Message message, ElvinError *error)
{
  Keys *keys = elvin_keys_create ();

  elvin_keys_read (buffer, keys, error);

  *(Keys **)message = keys;
}

void write_keys (ByteBuffer *buffer, Message message, ElvinError *error)
{
  elvin_keys_write (buffer, *(Keys **)message, error);
}

/* TODO limit items */
void read_values (ByteBuffer *buffer, Message message, ElvinError *error)
{
  uint32_t item_count = byte_buffer_read_int32 (buffer, error);

  if (elvin_error_ok (error))
  {
    Array *array = array_create (Value, item_count);
    Value *value = array->items;

    array->item_count = 0;

    for ( ; array->item_count < item_count && elvin_error_ok (error); value++)
    {
      if (value_read (buffer, value, error))
        array->item_count++;
    }

    *(Array **)message = array;
  }
}

void write_values (ByteBuffer *buffer, Message message, ElvinError *error)
{
  size_t item_count = (*(Array **)message)->item_count;
  Value *value = (Value *)(*(Array **)message)->items;

  byte_buffer_write_int32 (buffer, item_count, error);

  for ( ; item_count > 0 && elvin_error_ok (error); item_count--, value++)
    value_write (buffer, value, error);
}

void values_free (Array *values)
{
  size_t i;
  Value *value = values->items;

  for (i = values->item_count; i > 0; i--, value++)
    value_free (value);

  array_free (values);
}
