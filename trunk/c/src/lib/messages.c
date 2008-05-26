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

typedef struct
{
  MessageTypeID id;
  const char *field_types;
  const char *field_names [16];
} MessageFormat;

/* 
 * XI = XID
 * I4 = int 32
 * I8 = int 64
 * R8 = real 64
 * ST = string
 * VA = value array
 * NV = named values
 * KY = keys
 */ 
static MessageFormat message_formats [] = 
{
  {MESSAGE_ID_NACK,
    "XI I4 ST VA ", {"xid", "error", "message", "args"}},
  {MESSAGE_ID_CONN_RQST,
    "XI I4 I4 NV KY KY ", 
    {"xid", "version_major", "version_minor",
     "connection_options", "notification_keys" "subscription_keys"}},
  {MESSAGE_ID_CONN_RPLY,
    "XI NV ", {"xid", "connection_options"}},
  {MESSAGE_ID_DISCONN_RQST,
    "XI ", {"xid"}},
  {MESSAGE_ID_DISCONN_RPLY,
    "XI ", {"xid"}},
  {-1, "", {"none"}}
};

/** Create an integer ID from a two-letter field type */
#define field_id_of(field) ((field [0] << 8) + field [1])

/** Create a constant integer field ID from a two-letter field type */
#define field_id(c1,c2) ((c1 << 8) + c2)

#define FIELD_TYPE_INT32        (field_id ('I', '4'))
#define FIELD_TYPE_INT64        (field_id ('I', '8'))
#define FIELD_TYPE_REAL64       (field_id ('R', '8'))
#define FIELD_TYPE_VALUE_ARRAY  (field_id ('V', 'A'))
#define FIELD_TYPE_NAMED_VALUES (field_id ('N', 'V'))
#define FIELD_TYPE_KEYS         (field_id ('K', 'Y'))
#define FIELD_TYPE_STRING       (field_id ('S', 'T'))
#define FIELD_TYPE_XID          (field_id ('X', 'I'))

static MessageFormat *message_format_for (MessageTypeID type);

static Message read_using_format (ByteBuffer *buffer, 
                                  Message message,
                                  MessageFormat *format, 
                                  ElvinError *error);

static bool write_using_format (ByteBuffer *buffer, 
                                MessageFormat *format,
                                Message message,
                                ElvinError *error);

static uint32_t global_xid_counter = 0;

/* todo this is not thread safe */
#define next_xid() (++global_xid_counter)

Message message_init (Message message, MessageTypeID type, ...)
{
  MessageFormat *format = message_format_for (type);
  va_list args;
  const char *field;

  assert (format != NULL);

  message_type_of (message) = type;
  
  message += 4;  
  
  va_start (args, type);
  
  for (field = format->field_types; *field; field += 3)
  {
    switch (field_id_of (field))
    {
    case FIELD_TYPE_INT32:
      *(uint32_t *)message = va_arg (args, uint32_t);
      message += 4;
      break;
    case FIELD_TYPE_XID:
      *(uint32_t *)message = next_xid ();
      message += 4;
      break;
    case FIELD_TYPE_STRING:
      /* TODO */
      va_arg (args, char *);
      message += sizeof (char *);
      break;
    case FIELD_TYPE_NAMED_VALUES:
      /* TODO */
      va_arg (args, NamedValues *);
      message += sizeof (NamedValues *);
      break;
    case FIELD_TYPE_KEYS:
      /* TODO */
      va_arg (args, Keys *);
      message += sizeof (Keys *);
      break;
    default:
      /* TODO */
      abort ();
    }
  }
  
  va_end (args);
  
  return message;
}

/**
 * Destroy a message. Will not free any child data.
 */
void message_destroy (Message message)
{
  free (message);
  
  /* todo destroy subfields */
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

  /* TODO */
/*  if (buffer->position < buffer->max_data_length)
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Message underflow"); */
  
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
  byte_buffer_write_int32 (buffer, frame_size, error);
  
  byte_buffer_set_position (buffer, frame_size + 4, error);
  
  return true; 
}

Message read_using_format (ByteBuffer *buffer, 
                           Message message,
                           MessageFormat *format, 
                           ElvinError *error)
{
  const char *field;
  Message message_field = message;
   
  for (field = format->field_types; 
       elvin_error_ok (error) && *field; field += 3)
  {
    switch (field_id_of (field))
    {
    case FIELD_TYPE_INT32:
      *(uint32_t *)message_field = byte_buffer_read_int32 (buffer, error);
      message_field += 4;
      break;
    case FIELD_TYPE_XID:
      *(uint32_t *)message_field = byte_buffer_read_int32 (buffer, error);
      
      if (elvin_error_ok (error))
      {
        if (*(uint32_t *)message_field <= 0)
          elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "XID cannot be <= 0");
      }
      message_field += 4;
      
      break;
    case FIELD_TYPE_STRING:
      /* TODO */
      byte_buffer_skip (buffer, 4, error);
      message_field += sizeof (char *);
      break;
    case FIELD_TYPE_NAMED_VALUES:
      /* TODO */
      byte_buffer_skip (buffer, 4, error);
      message_field += sizeof (NamedValues *);
      break;
    case FIELD_TYPE_KEYS:
      /* TODO */
      byte_buffer_skip (buffer, 4, error);
      message_field += sizeof (Keys *);
      break;
    default:
      abort ();
    }
  }
  
  if (elvin_error_ok (error))
  {
    return message;
  } else
  {
    free (message);
    
    return NULL;
  }
}

bool write_using_format (ByteBuffer *buffer, 
                         MessageFormat *format,
                         Message message,
                         ElvinError *error)
{
  const char *field;
  
  for (field = format->field_types; 
       elvin_error_ok (error) && *field; field += 3)
  {
    switch (field_id_of (field))
    {
    case FIELD_TYPE_INT32:
    case FIELD_TYPE_XID:
      byte_buffer_write_int32 (buffer, *(uint32_t *)message, error);
      message += 4;      
      break;
    case FIELD_TYPE_STRING:
      /* TODO */
      byte_buffer_write_int32 (buffer, 0, error);
      message += sizeof (char *);
      break;
    case FIELD_TYPE_NAMED_VALUES:
      /* TODO */
      byte_buffer_write_int32 (buffer, 0, error);
      message += sizeof (NamedValues *);
      break;
    case FIELD_TYPE_KEYS:
      /* TODO */
      byte_buffer_write_int32 (buffer, 0, error);
      message += sizeof (Keys *);
      break;
    default:
      abort ();
    }
  }

  return elvin_error_ok (error);
}

MessageFormat *message_format_for (MessageTypeID type)
{
  int i;
  
  for (i = 0; message_formats [i].id != -1; i++)
  {
    if (message_formats [i].id == type)
      return &message_formats [i];
  }
  
  DIAGNOSTIC1 ("Failed to lookup info for message type %i", type);
  
  return NULL;
}