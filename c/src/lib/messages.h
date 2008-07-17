/*
 *  Avis Elvin client library for C.
 *
 *  Copyright (C) 2008 Matthew Phillips <avis@mattp.name>
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of version 3 of the GNU Lesser General
 *  Public License as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
#ifndef ELVIN_MESSAGES_H
#define ELVIN_MESSAGES_H

#include <avis/stdtypes.h>
#include <avis/attributes.h>
#include <avis/keys.h>

#include "byte_buffer.h"

/** Max number of fields in a message. */
#define MAX_MESSAGE_FIELDS 8

/** Max size of an in-memory decoded message */
#define MAX_MESSAGE_SIZE (MAX_MESSAGE_FIELDS * sizeof (int *))

/**
 * Message type ID's for the subset of Elvin messages understood by the
 * client.
 */
typedef enum
{
  MESSAGE_ID_NACK           = 48,
  MESSAGE_ID_CONN_RQST      = 49,
  MESSAGE_ID_CONN_RPLY      = 50,
  MESSAGE_ID_DISCONN_RQST   = 51,
  MESSAGE_ID_DISCONN_RPLY   = 52,
  MESSAGE_ID_DISCONN        = 53,
  MESSAGE_ID_SEC_RQST       = 54,
  MESSAGE_ID_SEC_RPLY       = 55,
  MESSAGE_ID_NOTIFY_EMIT    = 56,
  MESSAGE_ID_NOTIFY_DELIVER = 57,
  MESSAGE_ID_SUB_ADD_RQST   = 58,
  MESSAGE_ID_SUB_MOD_RQST   = 59,
  MESSAGE_ID_SUB_DEL_RQST   = 60,
  MESSAGE_ID_SUB_RPLY       = 61
} MessageTypeID;

/**
 * Selected NACK codes.
 */
typedef enum
{
  NACK_PROT_INCOMPAT  = 0001,
  NACK_PROT_ERROR     = 1001,
  NACK_NO_SUCH_SUB    = 1002,
  NACK_IMPL_LIMIT     = 2006,
  NACK_NOT_IMPL       = 2007,
  NACK_PARSE_ERROR    = 2101,
  NACK_EXP_IS_TRIVIAL = 2110
} NackCode;

/**
 * A message is actually a fixed-length blob 'o bytes up to MAX_MESSAGE_SIZE
 * long.
 */
typedef uint8_t * Message;

/**
 * Allocate a message on the stack.
 */
#define alloc_message(name) uint8_t name [MAX_MESSAGE_SIZE]

/**
 * Initialise a message's fields from a variable length set of arguments.
 */
Message message_init (Message message, MessageTypeID type, ...);

#define message_destroy(message) \
  (message_free (message), free (message), message = NULL)

/**
 * Free fields allocated inside a message dynamically allocated by
 * message_read().
 */
void message_free (Message message);

/**
 * Read an XDR-encoded message from a buffer. The buffer's max length must be
 * primed with the amount of data expected to be read and the position set to
 * the start of the data.
 *
 * @see message_read()
 * @see message_free()
 */
bool message_read (ByteBuffer *buffer, Message message, ElvinError *error);

/**
 * Write a message to a buffer in Elvin XDR-encoded form.
 */
bool message_write (ByteBuffer *buffer, Message message, ElvinError *error);

bool send_message (socket_t socket, Message message, ElvinError *error);

bool receive_message (socket_t socket, Message message, ElvinError *error);

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
