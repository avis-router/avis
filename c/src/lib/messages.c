#include <stdio.h>
#include <stdbool.h>
#include <stdbool.h>

#include <elvin/errors.h>

#include "messages.h"
#include "byte_buffer.h"

/**
 * Generic type for a message to access its type field.
 */
typedef struct
{
  MessageID type;
} Message;

typedef bool(*MessageWriteFunc) (ByteBuffer *, void *, ElvinError *);

typedef void *(*MessageReadFunc) (ByteBuffer *, ElvinError *);


static MessageWriteFunc lookup_write_function (MessageID type);

static MessageReadFunc lookup_read_function (MessageID type);

static void *ConnRqst_read (ByteBuffer *buffer, ElvinError *error);

static bool ConnRqst_write (ByteBuffer *buffer,
                            void *connRqst, ElvinError *error);

static void *ConnRply_read (ByteBuffer *buffer, ElvinError *error);

static bool ConnRply_write (ByteBuffer *buffer,
                            void *connRply, ElvinError *error);

static void *DisconnRply_read (ByteBuffer *buffer, ElvinError *error);

static bool DisconnRply_write (ByteBuffer *buffer,
                               void *message, ElvinError *error);

static void *DisconnRqst_read (ByteBuffer *buffer, ElvinError *error);

static bool DisconnRqst_write (ByteBuffer *buffer,
                               void *message, ElvinError *error);

static void *Nack_read (ByteBuffer *buffer, ElvinError *error);

static bool Nack_write (ByteBuffer *buffer, void *message, ElvinError *error);

static uint32_t global_xid_counter = 0;

// todo this is not thread safe
#define next_xid() (++global_xid_counter)

/**
 * Destroy a message. Will not free any child data.
 */
void message_destroy (void *message)
{
  free (message);
  
  // todo destroy subfields
}

/**
 * Read a message from a buffer. The buffer's max length must be primed with
 * the amount of data expected to be read and the position set to the start
 * of the data.
 */
bool message_read (ByteBuffer *buffer, void **message, ElvinError *error)
{
  uint32_t type;
  
  error_return (byte_buffer_read_int32 (buffer, &type, error));
  
  MessageReadFunc reader = lookup_read_function (type);
  
  if (reader == NULL)
  {
    return elvin_error_set (error, ELVIN_ERROR_PROTOCOL, 
                            "Unknown message type");
  }
  
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

bool message_write (ByteBuffer *buffer, void *message, ElvinError *error)
{
  MessageID type = ((Message *)message)->type;
  MessageWriteFunc writer = lookup_write_function (type);
  
  if (writer == NULL)
  {
    return elvin_error_set (error, ELVIN_ERROR_INTERNAL, 
                            "Unknown message type");
  }

  error_return (byte_buffer_skip (buffer, 4, error));
  error_return (byte_buffer_write_int32 (buffer, type, error));
  
  if (!(*writer) (buffer, message, error))
    return false;
  
  size_t frame_size = buffer->position - 4;

  // write frame length
  byte_buffer_set_position (buffer, 0, error);
  byte_buffer_write_int32 (buffer, frame_size, error);
  
  byte_buffer_set_position (buffer, frame_size + 4, error);
  
  return true; 
}

MessageWriteFunc lookup_write_function (MessageID type)
{
  switch (type)
  {
  case MESSAGE_ID_NACK:
      return Nack_write;
  case MESSAGE_ID_CONN_RQST:
    return ConnRqst_write;
  case MESSAGE_ID_CONN_RPLY:
    return ConnRply_write;
  case MESSAGE_ID_DISCONN_RQST:
    return DisconnRqst_write;
  case MESSAGE_ID_DISCONN_RPLY:
    return DisconnRply_write;
  default:
    return NULL;
  }
}

MessageReadFunc lookup_read_function (MessageID type)
{
  switch (type)
  {
  case MESSAGE_ID_NACK:
        return Nack_read;
  case MESSAGE_ID_CONN_RQST:
    return ConnRqst_read;
  case MESSAGE_ID_CONN_RPLY:
    return ConnRply_read;
  case MESSAGE_ID_DISCONN_RQST:
    return DisconnRqst_read;
  case MESSAGE_ID_DISCONN_RPLY:
    return DisconnRply_read;
  default:
    return NULL;
  }
}

//////

ConnRqst *ConnRqst_init (ConnRqst *connRqst,
                         uint8_t version_major, uint8_t version_minor,
                         NamedValues *connection_options,
                         Keys *notification_keys, Keys *subscription_keys)
{
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
  // todo free sub-fields
}

bool ConnRqst_write (ByteBuffer *buffer,
                     void *connRqst, ElvinError *error)
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

void *ConnRqst_read (ByteBuffer *buffer, ElvinError *error)
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

ConnRply *ConnRply_init (ConnRply *connRply, NamedValues *connection_options)
{
  connRply->xid = 0;
  connRply->type = MESSAGE_ID_CONN_RPLY;
  connRply->options = connection_options;
  
  return connRply;
}

void ConnRply_destroy (ConnRply *connRply)
{
  // todo free sub-fields
}

bool ConnRply_write (ByteBuffer *buffer,
                     void *connRply, ElvinError *error)
{
  error_return (byte_buffer_write_int32 
                   (buffer, ((ConnRply *)connRply)->xid, error));

  // TODO options
  byte_buffer_write_int32 (buffer, 0, error);
  
  return true;
}

void *ConnRply_read (ByteBuffer *buffer, ElvinError *error)
{
  ConnRply *connRply = (ConnRply *)malloc (sizeof (ConnRply));

  // todo dealloc on error
  error_return (byte_buffer_read_int32 
                 (buffer, &((ConnRply *)connRply)->xid, error));
  // TODO options
  byte_buffer_skip (buffer, 4, error);
  
  return connRply;
}

///////

void DisconnRqst_init (DisconnRqst *disconnRqst)
{
  disconnRqst->type = MESSAGE_ID_DISCONN_RQST;
  disconnRqst->xid = next_xid ();
}

void *DisconnRqst_read (ByteBuffer *buffer, ElvinError *error)
{
  DisconnRqst *message = (DisconnRqst *)malloc (sizeof (DisconnRqst));
  
  // todo dealloc on error
  error_return (byte_buffer_read_int32 
                 (buffer, &((DisconnRqst *)message)->xid, error));

  return message;
}

bool DisconnRqst_write (ByteBuffer *buffer, void *message, ElvinError *error)
{
  error_return (byte_buffer_write_int32 
                   (buffer, ((DisconnRqst *)message)->xid, error));
  
  return true;
}

//////

void DisconnRply_init (DisconnRply *disconnRply)
{
  disconnRply->type = MESSAGE_ID_DISCONN_RPLY;
  disconnRply->xid = 0;
}

void *DisconnRply_read (ByteBuffer *buffer, ElvinError *error)
{
  DisconnRply *message = (DisconnRply *)malloc (sizeof (DisconnRply));
    
  // todo dealloc on error
  error_return (byte_buffer_read_int32 
                 (buffer, &((DisconnRply *)message)->xid, error));

  return message;
}

bool DisconnRply_write (ByteBuffer *buffer, void *message, ElvinError *error)
{
  error_return (byte_buffer_write_int32 
                   (buffer, ((DisconnRply *)message)->xid, error));
  
  return true;
}

//////

void Nack_init (Nack *nack, uint32_t error, const char *message)
{
  nack->type = MESSAGE_ID_NACK;
  nack->xid = next_xid ();
  nack->error = error;
  nack->message = message;
  nack->args = NULL;
}

void *Nack_read (ByteBuffer *buffer, ElvinError *error)
{
  Nack *message = malloc (sizeof (Nack));
    
  // todo dealloc on error
  error_return (byte_buffer_read_int32 
                 (buffer, &((Nack *)message)->xid, error));
  error_return (byte_buffer_read_int32 
                 (buffer, &((Nack *)message)->error, error));

  // todo message and args
  byte_buffer_skip (buffer, 4, error);

  return message;
}

bool Nack_write (ByteBuffer *buffer, void *message, ElvinError *error)
{
  error_return (byte_buffer_write_int32 
                   (buffer, ((Nack *)message)->xid, error));
  error_return (byte_buffer_write_int32 
                   (buffer, ((Nack *)message)->xid, error));
  
  // todo message and args
  error_return (byte_buffer_write_int32 (buffer, 0, error));
  error_return (byte_buffer_write_int32 (buffer, 0, error));
  
  return true;
}