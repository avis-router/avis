#ifndef ELVIN_MESSAGES_H
#define ELVIN_MESSAGES_H

#include <stdint.h>
#include <stdbool.h>

#include <elvin/named_values.h>
#include <elvin/keys.h>
#include <elvin/elvin.h>

#include "byte_buffer.h"

typedef enum
{
  MESSAGE_ID_NACK = 48,
  MESSAGE_ID_CONN_RQST = 49,
  MESSAGE_ID_CONN_RPLY = 50,
  MESSAGE_ID_DISCONN_RQST = 51,
  MESSAGE_ID_DISCONN_RPLY = 52
} Message_Id;

typedef struct
{
  Message_Id type;
  uint32_t xid;
  uint32_t error;
  const char *message;
  void **args;
} Nack;

typedef struct
{
  Message_Id type;
  uint32_t xid;
  uint32_t version_major;
  uint32_t version_minor;
  NamedValues *connection_options;
  Keys *notification_keys;
  Keys *subscription_keys;
} ConnRqst;

typedef struct
{
  Message_Id type;
  uint32_t xid;
  NamedValues *options;
} ConnRply;

typedef struct
{
  Message_Id type;
  uint32_t reason;
  const char *message;
} Disconn;

typedef struct
{
  Message_Id type;
  uint32_t xid;
} DisconnRqst;

typedef struct
{
  Message_Id type;
  uint32_t xid;
} DisconnRply;

/**
 * The general header of a message, including XID (which is not present on
 * all messages. This is only intended to be used internally. 
 */
typedef struct
{
  Message_Id type;
  uint32_t xid;
} XidMessage;

bool message_read (ByteBuffer *buffer, void **message, ElvinError *error);

bool message_write (ByteBuffer *buffer, void *message, ElvinError *error);

void message_destroy (void *message);

// destroy and free () message
#define message_free(message) (message_destroy (message), free (message))

#define message_type_of(message) (((XidMessage *)message)->type)
#define xid_of(message) (((XidMessage *)message)->xid)

ConnRqst *ConnRqst_init (ConnRqst *connRqst,
                         uint8_t version_major, uint8_t version_minor,
                         NamedValues *connection_options,
                         Keys *notification_keys, Keys *subscription_keys);

void ConnRqst_destroy (ConnRqst *connRqst);

ConnRply *ConnRply_create (NamedValues *connection_options);

void ConnRply_destroy (ConnRply *connRply);

void DisconnRqst_init (DisconnRqst *disconnRqst);

void DisconnRply_init (DisconnRply *disconnRply);

#endif // ELVIN_MESSAGES_H