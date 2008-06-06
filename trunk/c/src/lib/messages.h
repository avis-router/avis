#ifndef ELVIN_MESSAGES_H
#define ELVIN_MESSAGES_H

#ifdef WIN32
  #include <malloc.h>
#else
  #include <alloca.h>
#endif

#include <elvin/stdtypes.h>
#include <elvin/named_values.h>
#include <elvin/keys.h>
#include <elvin/elvin.h>

#include "byte_buffer.h"

/** Max number of fields in a message. */
#define MAX_MESSAGE_FIELDS 8

/** Max size of an in-memory decoded message */
#define MAX_MESSAGE_SIZE (MAX_MESSAGE_FIELDS * sizeof (int *))

typedef enum
{
  MESSAGE_ID_NACK = 48,
  MESSAGE_ID_CONN_RQST = 49,
  MESSAGE_ID_CONN_RPLY = 50,
  MESSAGE_ID_DISCONN_RQST = 51,
  MESSAGE_ID_DISCONN_RPLY = 52,
  MESSAGE_ID_DISCONN = 53,
  MESSAGE_ID_NOTIFY_EMIT = 56,
  MESSAGE_ID_NOTIFY_DELIVER = 57,
  MESSAGE_ID_SUB_ADD_RQST = 58,
  MESSAGE_ID_SUB_DEL_RQST = 60,
  MESSAGE_ID_SUB_RPLY = 61
} MessageTypeID;

typedef uint8_t * Message;

#define message_alloca() ((Message)alloca (MAX_MESSAGE_SIZE))

Message message_init (Message message, MessageTypeID type, ...);

#define message_destroy(message) \
  (message_free (message), free (message), message = NULL)

/**
 * Free memory allocated to a message dynamically allocated by message_read().
 */
void message_free (Message message);

/**
 * Read a message from a buffer. The buffer's max length must be primed with
 * the amount of data expected to be read and the position set to the start
 * of the data.
 * 
 * @see message_free()
 */
bool message_read (ByteBuffer *buffer, Message message, ElvinError *error);

bool message_write (ByteBuffer *buffer, Message message, ElvinError *error);

/** The message's type ID. */
#define message_type_of(message) (*(uint32_t *)(message))

/** The message's transaction ID. */
#define xid_of(message) (int32_at_offset (message, 0))

#define int32_at_offset(message, offset) \
  (*(int32_t *)((message) + ((offset) + 4)))

#define int64_at_offset(message, offset) \
  (*(int64_t *)((message) + ((offset) + 4)))

#define int64s_at_offset(message, offset) \
  (*(int64_t **)(message + ((offset) + 4)))

#define ptr_at_offset(message, offset) \
  (*(void **)(message + ((offset) + 4)))

#endif /* ELVIN_MESSAGES_H */
