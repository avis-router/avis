#ifndef ELVIN_MESSAGES_H
#define ELVIN_MESSAGES_H

#ifdef WIN32
#define alloca _alloca
#endif //!WIN32

#include <elvin/stdtypes.h>
#include <elvin/named_values.h>
#include <elvin/keys.h>
#include <elvin/elvin.h>

#include "byte_buffer.h"

/** Max size of an in-memory decoded message */
#define MAX_MESSAGE_SIZE (16 * sizeof (int *))

typedef enum
{
  MESSAGE_ID_NACK = 48,
  MESSAGE_ID_CONN_RQST = 49,
  MESSAGE_ID_CONN_RPLY = 50,
  MESSAGE_ID_DISCONN_RQST = 51,
  MESSAGE_ID_DISCONN_RPLY = 52
} MessageTypeID;

typedef uint8_t * Message;

Message message_read (ByteBuffer *buffer, ElvinError *error);

bool message_write (ByteBuffer *buffer, Message message, ElvinError *error);

void message_destroy (Message message);

#define message_alloca() ((Message)alloca (MAX_MESSAGE_SIZE))

Message message_init (Message message, MessageTypeID type, ...);

/** Destroy and free () message */
#define message_free(message) (message_destroy (message), free (message))

/** The message's type ID. */
#define message_type_of(message) (*(uint32_t *)(message))

/** The message's transaction ID. */
#define xid_of(message) (*(uint32_t *)((message) + 4))

#endif /* ELVIN_MESSAGES_H */