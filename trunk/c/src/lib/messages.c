#include <stdlib.h>
#include <stdbool.h>

#include <elvin/errors.h>

#include "messages.h"
#include "byte_buffer.h"

typedef struct
{
  Message_Type type;
} Message;

typedef bool(*Message_Write_Func) (Byte_Buffer *, void *, Elvin_Error *);
typedef void *(*Message_Read_Func) (Byte_Buffer *, Elvin_Error *);

static Message_Write_Func lookup_write_function (Message_Type type);
static Message_Read_Func lookup_read_function (Message_Type type);

static void *ConnRqst_read (Byte_Buffer *buffer, Elvin_Error *error);
static bool ConnRqst_write (Byte_Buffer *buffer,
                            void *connRqst, Elvin_Error *error);

static uint32_t global_xid_counter = 0;

// todo this is not thread safe
#define next_xid() (++global_xid_counter)

bool message_read (Byte_Buffer *buffer, void **message, Elvin_Error *error)
{
  uint32_t frame_length;
  uint32_t type;
  
  error_return (byte_buffer_read_int32 (buffer, &frame_length, error));
  
  // todo check frame size
  byte_buffer_ensure_capacity (buffer, frame_length);
  byte_buffer_set_max_length (buffer, frame_length);
  
  error_return (byte_buffer_read_int32 (buffer, &type, error));
  
  Message_Read_Func reader = lookup_read_function (type);
  
  if (reader == NULL)
    return elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Unknown message type");
  
  *message = (*reader) (buffer, error);
  
  // fill in type field
  ((Message *)*message)->type = type;
  
  if (buffer->position != frame_length + 4)
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Message underflow");
  
  return elvin_error_ok (error);
}

bool message_write (Byte_Buffer *buffer, void *message, Elvin_Error *error)
{
  Message_Type type = ((Message *)message)->type;
  Message_Write_Func writer = lookup_write_function (type);
  
  if (writer == NULL)
    return elvin_error_set (error, ELVIN_ERROR_INTERNAL, "Unknown message type");

  error_return (byte_buffer_skip (buffer, 4, error));
  error_return (byte_buffer_write_int32 (buffer, type, error));
  
  if (!(*writer) (buffer, message, error))
    return false;
  
  size_t frame_size = byte_buffer_position (buffer) - 4;
  
  byte_buffer_set_position (buffer, 0, error);
  byte_buffer_write_int32 (buffer, frame_size, error);
  
  byte_buffer_set_position (buffer, frame_size + 4, error);
  
  return true; 
}

ConnRqst *ConnRqst_create (uint8_t version_major, uint8_t version_minor,
                           Named_Values *connection_options,
                           Keys *notification_keys, Keys *subscription_keys)
{
  ConnRqst *connRqst = (ConnRqst *)malloc (sizeof (ConnRqst));
  
  connRqst->xid = next_xid ();
  connRqst->type = MESSAGE_CONN_RQST;
  connRqst->version_major = version_major;
  connRqst->version_major = version_minor;
  connRqst->connection_options = connection_options;
  connRqst->subscription_keys = subscription_keys;
  connRqst->notification_keys = notification_keys;
  
  return connRqst;
}

void ConnRqst_destroy (ConnRqst *connRqst)
{
  free (connRqst);
  
  // todo free sub-fields?
}

bool ConnRqst_write (Byte_Buffer *buffer,
                     void *connRqst, Elvin_Error *error)
{
  error_return (byte_buffer_write_int32 
                   (buffer, ((ConnRqst *)connRqst)->xid, error));
  error_return (byte_buffer_write_int32 
                 (buffer, ((ConnRqst *)connRqst)->version_major, error));
  error_return (byte_buffer_write_int32 
                   (buffer, ((ConnRqst *)connRqst)->version_minor, error));
  
  // TODO options, keys
  byte_buffer_write_int32 (buffer, 0, error);
  byte_buffer_write_int32 (buffer, 0, error);
  byte_buffer_write_int32 (buffer, 0, error);
  
  return true;
}

void *ConnRqst_read (Byte_Buffer *buffer, Elvin_Error *error)
{
  ConnRqst *connRqst = (ConnRqst *)malloc (sizeof (ConnRqst));
  
  error_return (byte_buffer_read_int32 
                 (buffer, &((ConnRqst *)connRqst)->xid, error));
  error_return (byte_buffer_read_int32 
                 (buffer, &((ConnRqst *)connRqst)->version_major, error));
  error_return (byte_buffer_read_int32 
                 (buffer, &((ConnRqst *)connRqst)->version_minor, error));
  
  // TODO options, keys
  byte_buffer_skip (buffer, 4 * 3, error);
  
  return connRqst;
}

Message_Write_Func lookup_write_function (Message_Type type)
{
  switch (type)
  {
  case MESSAGE_CONN_RQST:
    return ConnRqst_write;
  default:
    return NULL;
  }
}

Message_Read_Func lookup_read_function (Message_Type type)
{
  switch (type)
  {
  case MESSAGE_CONN_RQST:
    return ConnRqst_read;
  default:
    return NULL;
  }
}

//#define field_int32(_value) (Field){.type = FIELD_INT32, .value = {.value_int32 = _value}}
//#define field_named_values(_value) (Field){.type = FIELD_NAMED_VALUES, .value = {.value_named_values = _value}}
//#define field_keys(_value) (Field){.type = FIELD_KEYS, .value = {.value_keys = _value}}
//
//static Message *message_alloc (Message_Type type);
//static int field_count (Message_Type type);
//
//Message *ConnRqst_create (short version_major, short version_minor,
//					      Named_Values *connection_options,
//						  Keys *notification_keys, Keys *subscription_keys)
//{
//  Message *message = message_alloc (MESSAGE_CONN_RQST);
//  
//  message->fields [0] = field_int32 (version_major);
//  message->fields [1] = field_int32 (version_minor);
//  message->fields [2] = field_named_values (connection_options);
//  message->fields [3] = field_keys (notification_keys);
//  message->fields [4] = field_keys (subscription_keys);
//    
//  return message;
//}
//
//static Message *message_alloc (Message_Type type)
//{
//  Message *message = malloc (sizeof (Message));
//
//  message->type = type;  
//  message->fields = (Field *)malloc (field_count (type) * sizeof (Field));
//  
//  return message;
//}
//
//static int field_count (Message_Type type)
//{
//  switch (type)
//  {
//    case MESSAGE_CONN_RQST:
//	  return 5;
//    default:
//	  abort ();
//  }
//}
