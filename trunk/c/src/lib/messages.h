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
  MESSAGE_ID_CONN_RQST = 49,
  MESSAGE_ID_CONN_RPLY = 50,
  MESSAGE_ID_DISCONN_RQST = 51,
  MESSAGE_ID_DISCONN_RPLY = 52
} Message_Id;

typedef struct
{
  Message_Id type;
  uint32_t xid;
  uint32_t version_major;
  uint32_t version_minor;
  Named_Values *connection_options;
  Keys *notification_keys;
  Keys *subscription_keys;
} ConnRqst;

typedef struct
{
  Message_Id type;
  uint32_t xid;
  Named_Values *options;
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

bool message_read (Byte_Buffer *buffer, void **message, Elvin_Error *error);

bool message_write (Byte_Buffer *buffer, void *message, Elvin_Error *error);

void message_destroy (void *message);

ConnRqst *ConnRqst_create (uint8_t version_major, uint8_t version_minor,
                           Named_Values *connection_options,
                           Keys *notification_keys, Keys *subscription_keys);

void ConnRqst_destroy (ConnRqst *connRqst);

ConnRply *ConnRply_create (Named_Values *connection_options);

void ConnRply_destroy (ConnRply *connRply);

void DisconnRqst_init (DisconnRqst *disconnRqst);

void DisconnRply_init (DisconnRply *disconnRply);

#endif // ELVIN_MESSAGES_H