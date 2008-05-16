#include <stdio.h>
#include <stdbool.h>
#include <stdbool.h>

#include <elvin/errors.h>

#include "messages.h"
#include "byte_buffer.h"

typedef struct
{
  Message_Id type;
} Message;

typedef bool(*Message_Write_Func) (Byte_Buffer *, void *, Elvin_Error *);
typedef void *(*Message_Read_Func) (Byte_Buffer *, Elvin_Error *);

static Message_Write_Func lookup_write_function (Message_Id type);
static Message_Read_Func lookup_read_function (Message_Id type);

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
  byte_buffer_ensure_capacity (buffer, frame_length + 4);
  byte_buffer_set_max_length (buffer, frame_length + 4);
  
  error_return (byte_buffer_read_int32 (buffer, &type, error));
  
  Message_Read_Func reader = lookup_read_function (type);
  
  if (reader == NULL)
    return elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Unknown message type");
  
  void *new_message = (*reader) (buffer, error);
  
  if (!elvin_error_ok (error))
    return false;
    
  // fill in type field
  ((Message *)new_message)->type = type;

  *message = new_message;  
  
  // todo
//  if (buffer->position < buffer->max_data_length)
//    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Message underflow");
  
  return elvin_error_ok (error);
}

bool message_write (Byte_Buffer *buffer, void *message, Elvin_Error *error)
{
  Message_Id type = ((Message *)message)->type;
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
  connRqst->type = MESSAGE_ID_CONN_RQST;
  connRqst->version_major = version_major;
  connRqst->version_minor = version_minor;
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

/////////

ConnRply *ConnRply_create (Named_Values *connection_options)
{
  ConnRply *connRply = (ConnRply *)malloc (sizeof (ConnRply));
  
  connRply->xid = 0;
  connRply->type = MESSAGE_ID_CONN_RPLY;
  connRply->options = connection_options;
  
  return connRply;
}

void ConnRply_destroy (ConnRply *connRply)
{
  free (connRply);
  
  // todo free sub-fields?
}

bool ConnRply_write (Byte_Buffer *buffer,
                     void *connRply, Elvin_Error *error)
{
  error_return (byte_buffer_write_int32 
                   (buffer, ((ConnRply *)connRply)->xid, error));

  // TODO options
  byte_buffer_write_int32 (buffer, 0, error);
  
  return true;
}

void *ConnRply_read (Byte_Buffer *buffer, Elvin_Error *error)
{
  ConnRply *connRply = (ConnRply *)malloc (sizeof (ConnRply));
  
  error_return (byte_buffer_read_int32 
                 (buffer, &((ConnRply *)connRply)->xid, error));
  // TODO options
  byte_buffer_skip (buffer, 4, error);
  
  return connRply;
}

////////

Message_Write_Func lookup_write_function (Message_Id type)
{
  switch (type)
  {
  case MESSAGE_ID_CONN_RQST:
    return ConnRqst_write;
  case MESSAGE_ID_CONN_RPLY:
      return ConnRply_write;
  default:
    return NULL;
  }
}

Message_Read_Func lookup_read_function (Message_Id type)
{
  switch (type)
  {
  case MESSAGE_ID_CONN_RQST:
    return ConnRqst_read;
  case MESSAGE_ID_CONN_RPLY:
        return ConnRply_read;
  default:
    return NULL;
  }
}