#include <stdlib.h>
#include <stdbool.h>

#include "messages.h"

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

bool message_read (Byte_Buffer *buffer, void **message, Elvin_Error *error)
{
  uint32_t frame_length;
  uint32_t type;
  
  error_return (byte_buffer_read_int32 (buffer, &frame_length, error));
  
  // todo check frame size
  byte_buffer_set_capacity (buffer, frame_length);
  
  error_return (byte_buffer_read_int32 (buffer, &type, error));
  
  Message_Read_Func reader = lookup_read_function (type);
  
  if (reader == NULL)
    return elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Unknown message type");
  
  *message = (*reader) (buffer, error);
  
  return elvin_error_ok (error);
}

bool message_write (Byte_Buffer *buffer, void *message, Elvin_Error *error)
{
  Message_Type type = ((Message *)message)->type;
  Message_Write_Func writer = lookup_write_function (type);
  
  if (writer == NULL)
    return elvin_error_set (error, ELVIN_ERROR_INTERNAL, "Unknown message type");

  byte_buffer_allow_auto_resize (buffer, true);
  byte_buffer_skip (buffer, 4);
  byte_buffer_write_int32 (buffer, type);
  
  if (!(*writer) (buffer, message, error))
    return false;
  
  size_t frame_size = byte_buffer_position (buffer) - 4;
  
  byte_buffer_position (buffer, 0);
  byte_buffer_write_int32 (buffer, frame_size);
  
  return true; 
}

ConnRqst *ConnRqst_create (uint8_t version_major, uint8_t version_minor,
                           Named_Values *connection_options,
                           Keys *notification_keys, Keys *subscription_keys)
{
  // todo
  return NULL;  
}

bool ConnRqst_write (Byte_Buffer *buffer,
                     void *connRqst, Elvin_Error *error)
{
  // todo
  return true;
}

void *ConnRqst_read (Byte_Buffer *buffer, Elvin_Error *error)
{
  // todo
  return NULL;
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
