#include <stdlib.h>

#include "messages.h"

#define field_int32(_value) (Field){.type = FIELD_INT32, .value = {.value_int32 = _value}}
#define field_named_values(_value) (Field){.type = FIELD_NAMED_VALUES, .value = {.value_named_values = _value}}
#define field_keys(_value) (Field){.type = FIELD_KEYS, .value = {.value_keys = _value}}

static Message *message_alloc (Message_Type type);
static int field_count (Message_Type type);

Message *ConnRqst_create (short version_major, short version_minor,
					      Named_Values *connection_options,
						  Keys *notification_keys, Keys *subscription_keys)
{
  Message *message = message_alloc (MESSAGE_CONN_RQST);
  
  message->fields [0] = field_int32 (version_major);
  message->fields [1] = field_int32 (version_minor);
  message->fields [2] = field_named_values (connection_options);
  message->fields [3] = field_keys (notification_keys);
  message->fields [4] = field_keys (subscription_keys);
    
  return message;
}

static Message *message_alloc (Message_Type type)
{
  Message *message = malloc (sizeof (Message));

  message->type = type;  
  message->fields = (Field *)malloc (field_count (type) * sizeof (Field));
  
  return message;
}

static int field_count (Message_Type type)
{
  switch (type)
  {
    case MESSAGE_CONN_RQST:
	  return 5;
    default:
	  abort ();
  }
}
