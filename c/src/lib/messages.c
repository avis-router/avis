#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <assert.h>

#include <elvin/stdtypes.h>
#include <elvin/errors.h>
#include <elvin/named_values.h>
#include <elvin/log.h>

#include "messages.h"
#include "byte_buffer.h"
#include "named_values_private.h"

static Message read_int32 (ByteBuffer *buffer, Message message, 
                           ElvinError *error);
static Message write_int32 (ByteBuffer *buffer, Message message, 
                            ElvinError *error);

static Message read_bool (ByteBuffer *buffer, Message message, 
                          ElvinError *error);

static Message read_xid (ByteBuffer *buffer, Message message, 
                         ElvinError *error);

static Message read_string (ByteBuffer *buffer, Message message, 
                            ElvinError *error);
static Message write_string (ByteBuffer *buffer, Message message, 
                             ElvinError *error);

static Message read_named_values (ByteBuffer *buffer, Message message, 
                                  ElvinError *error);
static Message write_named_values (ByteBuffer *buffer, Message message, 
                                   ElvinError *error);

static Message read_values (ByteBuffer *buffer, Message message, 
                            ElvinError *error);
static Message write_values (ByteBuffer *buffer, Message message, 
                             ElvinError *error);

static Message read_keys (ByteBuffer *buffer, Message message, 
                          ElvinError *error);
static Message write_keys (ByteBuffer *buffer, Message message, 
                           ElvinError *error);

typedef enum {FIELD_INT32, FIELD_XID, FIELD_POINTER} FieldType;

typedef Message (*MessageIOFunction) (ByteBuffer *buffer, Message message, 
                 ElvinError *error);

typedef struct
{
  FieldType type;
  MessageIOFunction read;
  MessageIOFunction write;
} FieldFormat;

typedef struct
{
  MessageTypeID id;
  FieldFormat fields [32];
  const char *field_names [32];
} MessageFormat;

#define I4  {FIELD_INT32, read_int32, write_int32}
#define STR {FIELD_POINTER, read_string, write_string}
#define NV  {FIELD_POINTER, read_named_values, write_named_values}
#define BO  {FIELD_INT32, read_bool, write_int32}
#define XID {FIELD_XID, read_xid, write_int32}
#define VA  {FIELD_POINTER, read_values, write_values}
#define KY  {FIELD_POINTER, read_keys, write_keys}

#define END {0, (MessageIOFunction)NULL, (MessageIOFunction)NULL}

static MessageFormat MESSAGE_FORMATS [] = 
{
  {MESSAGE_ID_NACK,
    {XID, I4, STR, NV, VA, END}, {"xid", "error", "message", "args"}},
  {MESSAGE_ID_CONN_RQST,
    {XID, I4, I4, NV, KY, KY, END}, 
    {"xid", "version_major", "version_minor",
     "connection_options", "notification_keys" "subscription_keys"}},
  {MESSAGE_ID_CONN_RPLY,
    {XID, NV, END}, {"xid", "connection_options"}},
  {MESSAGE_ID_DISCONN_RQST,
    {XID, END}, {"xid"}},
  {MESSAGE_ID_DISCONN_RPLY,
    {XID, END}, {"xid"}},
  {MESSAGE_ID_NOTIFY_EMIT,
    {NV, BO, KY, END}, {"attributes", "deliverInsecure", "keys"}},
  {-1, {END}, {"none"}}
};

static MessageFormat *message_format_for (MessageTypeID type);

static Message read_using_format (ByteBuffer *buffer, 
                                  Message message,
                                  MessageFormat *format, 
                                  ElvinError *error);

static bool write_using_format (ByteBuffer *buffer, 
                                MessageFormat *format,
                                Message message,
                                ElvinError *error);

static uint32_t xid_counter = 1;

/* todo this is not thread safe */
#define next_xid() (xid_counter++)

void message_free (Message message)
{
  /* TODO free subfields */
}

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
    case FIELD_INT32:
      *(int32_t *)message = va_arg (args, int32_t);
      message += sizeof (int32_t);
      break;
    case FIELD_XID:
      *(uint32_t *)message = next_xid ();
      message += sizeof (uint32_t);
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

/**
 * Read a message from a buffer. The buffer's max length must be primed with
 * the amount of data expected to be read and the position set to the start
 * of the data.
 */
Message message_read (ByteBuffer *buffer, ElvinError *error)
{
  uint32_t type;
  Message message;
  MessageFormat *format;
  
  on_error_return (type = byte_buffer_read_int32 (buffer, error), NULL);
  
  format = message_format_for (type);
  
  if (format == NULL)
  {
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Unknown message type");
    
    return NULL;
  }
  
  message = malloc (MAX_MESSAGE_SIZE);
  
  /* fill in type field */
  message_type_of (message) = type;
    
  read_using_format (buffer, message + 4, format, error);
  
  if (elvin_error_occurred (error))
  {
    free (message);
    
    return NULL;
  }

  if (buffer->position < buffer->max_data_length)
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Message underflow");
  
  return message;
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
  
  if (!write_using_format (buffer, format, message + 4, error))
    return false;
  
  frame_size = buffer->position - 4;
  
  /* write frame length */
  byte_buffer_set_position (buffer, 0, error);
  byte_buffer_write_int32 (buffer, (uint32_t)frame_size, error);
  
  byte_buffer_set_position (buffer, frame_size + 4, error);
  
  return true; 
}

Message read_using_format (ByteBuffer *buffer, 
                           Message message,
                           MessageFormat *format, 
                           ElvinError *error)
{
  FieldFormat *field;
  
  for (field = format->fields; field->read && elvin_error_ok (error); field++)
    message = (*field->read) (buffer, message, error);

  if (elvin_error_occurred (error))
  {
    free (message);
    
    message = NULL;
  }
  
  return message;
}

bool write_using_format (ByteBuffer *buffer, 
                         MessageFormat *format,
                         Message message,
                         ElvinError *error)
{
  FieldFormat *field;
    
  for (field = format->fields; field->write && elvin_error_ok (error); field++)
    message = (*field->write) (buffer, message, error);

  return elvin_error_ok (error);
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

Message read_int32 (ByteBuffer *buffer, Message message, ElvinError *error)
{
  *(int32_t *)message = byte_buffer_read_int32 (buffer, error);
  
  return message + sizeof (int32_t);
}

Message write_int32 (ByteBuffer *buffer, Message message, ElvinError *error)
{
  byte_buffer_write_int32 (buffer, *(int32_t *)message, error);
    
  return message + sizeof (int32_t);
}

Message read_bool (ByteBuffer *buffer, Message message, ElvinError *error)
{
  int32_t value = byte_buffer_read_int32 (buffer, error);
  
  if (elvin_error_ok (error))
  {
    if (value == 0 || value == 1)
      *(int32_t *)message = value;
    else
      elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Invalid boolean");
  }
  
  return message + sizeof (int32_t);
}

Message read_xid (ByteBuffer *buffer, Message message, ElvinError *error)
{
  uint32_t value = byte_buffer_read_int32 (buffer, error);
    
  if (elvin_error_ok (error))
  {
    if (value > 0)
      *(uint32_t *)message = value;
    else
      elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Invalid XID");
  }
  
  return message + sizeof (uint32_t);
}

Message read_string (ByteBuffer *buffer, Message message, ElvinError *error)
{
  *(char **)message = byte_buffer_read_string (buffer, error);
  
  return message += sizeof (char *);
}

Message write_string (ByteBuffer *buffer, Message message, ElvinError *error)
{
  byte_buffer_write_string (buffer, *(char **)message, error);
    
  return message += sizeof (char *);
}

Message read_named_values (ByteBuffer *buffer, Message message, ElvinError *error)
{
  NamedValues *named_values = named_values_create ();
        
  named_values_read (buffer, named_values, error);

  if (elvin_error_ok (error))
    *(NamedValues **)message = named_values;
  else
    free (named_values);  
  
  return message + sizeof (NamedValues *);
}

Message write_named_values (ByteBuffer *buffer, Message message, ElvinError *error)
{
  named_values_write (buffer, *(NamedValues **)message, error);
  
  return message + sizeof (NamedValues *);
}

Message read_keys (ByteBuffer *buffer, Message message, ElvinError *error)
{
  /* TODO */
  byte_buffer_skip (buffer, 4, error);
  
  return message + sizeof (Keys *);
}

Message write_keys (ByteBuffer *buffer, Message message, ElvinError *error)
{
  /* TODO */ 
  byte_buffer_write_int32 (buffer, 0, error);
  
  return message + sizeof (Keys *);
}

Message read_values (ByteBuffer *buffer, Message message, ElvinError *error)
{
  /* TODO */
  abort ();
}

Message write_values (ByteBuffer *buffer, Message message, ElvinError *error)
{
  /* TODO */
  abort ();
}