#ifndef ELVIN_MESSAGES_H
#define ELVIN_MESSAGES_H

#include <stdint.h>
#include <stdbool.h>

#include <elvin/named_values.h>
#include <elvin/keys.h>
#include <elvin/elvin.h>

#include "byte_buffer.h"

//typedef enum
//{
//  FIELD_INT32, FIELD_NAMED_VALUES, FIELD_KEYS
//} Field_Type;
//
//typedef struct
//{
//  Field_Type type;
//  
//  union
//  {
//    uint32_t value_int32;
//	Named_Values *value_named_values;
//	Keys *value_keys;
//  } value;
//} Field;
//
//typedef enum
//{
//  MESSAGE_CONN_RQST = 49
//} Message_Type;
//
//typedef struct
//{
//  Message_Type type;
//  Field *fields;
//} Message;
//
//Message *ConnRqst_create (short version_major, short version_minor,
//					      Named_Values *connection_options,
//						  Keys *notification_keys, Keys *subscription_keys);

typedef enum
{
  MESSAGE_CONN_RQST = 49
} Message_Type;

typedef struct
{
  Message_Type type;
  uint8_t version_major;
  uint8_t version_minor;
  Named_Values *connection_options;
  Keys *notification_keys;
  Keys *subscription_keys;
} ConnRqst;

void *message_read (Byte_Buffer *buffer, Elvin_Error *error);

bool message_write (Byte_Buffer *buffer, void *message, Elvin_Error *error);

ConnRqst *ConnRqst_create (uint8_t version_major, uint8_t version_minor,
                           Named_Values *connection_options,
                           Keys *notification_keys, Keys *subscription_keys);

ConnRqst *ConnRqst_read (Byte_Buffer *buffer, Elvin_Error *error);

bool ConnRqst_write (ConnRqst *connRqst, Elvin_Error *error);

#endif // ELVIN_MESSAGES_H