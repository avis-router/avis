#include <avis/errors.h>
#include <avis/keys.h>

#include "keys_private.h"
#include "arrays_private.h"

Key avis_sha1 (Key input);

typedef Key (*HashFunc) (Key key);

#define PRODUCER 1
#define CONSUMER 2
#define DUAL     (PRODUCER + CONSUMER)

#define KEY_SCHEME_COUNT 3

struct KeyScheme
{
  int id;
  int type;
  HashFunc hash;
};

struct KeyScheme _KEY_SCHEME_SHA1_DUAL =     {1, DUAL,     avis_sha1};
struct KeyScheme _KEY_SCHEME_SHA1_PRODUCER = {2, PRODUCER, avis_sha1};
struct KeyScheme _KEY_SCHEME_SHA1_CONSUMER = {3, CONSUMER, avis_sha1};

static KeyScheme schemes [KEY_SCHEME_COUNT] = 
{
  &_KEY_SCHEME_SHA1_DUAL, 
  &_KEY_SCHEME_SHA1_PRODUCER, 
  &_KEY_SCHEME_SHA1_CONSUMER
};

static ArrayList _empty_dual_keys [2] = {{NULL, 0, 0}, {NULL, 0, 0}};

Keys _empty_keys = {{{&_empty_dual_keys, 0, 2}, {NULL, 0, 0}, {NULL, 0, 0}}};

#define index_for(scheme) ((scheme)->id - 1)

/** The key set (actually an array list) for a key collection and scheme ID */
#define keyset_for_scheme(k, id) (&(k->keys [(id) - 1]))

/** The single key list for a single key scheme. */
#define single_keyset(k, id) (keyset_for_scheme (k, id))

/** The dual producer key list for a key collection & scheme ID. */
#define dual_producer_keyset(k, id) \
  (ArrayList *)(keyset_for_scheme(k, id))->items 

/** The dual consumer key list for a key collection & scheme ID. */
#define dual_consumer_keyset(k, id) \
  (((ArrayList *)(keyset_for_scheme(k, id))->items) + 1) 

static KeyScheme scheme_for (uint32_t id, ElvinError *error);

static void free_keyset (ArrayList *keyset);

static bool keysets_equal (ArrayList *keyset1, ArrayList *keyset2);

static bool write_keyset (ByteBuffer *buffer, ArrayList *keyset, 
                          ElvinError *error);

static bool read_keyset (ByteBuffer *buffer, ArrayList *keyset, 
                         ElvinError *error);


Keys *elvin_keys_init (Keys *keys)
{
  /* init producer/consumer sub-lists for SHA1 dual scheme */
  array_list_init (&keys->keys [0], sizeof (ArrayList), 2);
  
  keys->keys [0].item_count = 2;
  
  array_list_init (dual_producer_keyset (keys, 1), sizeof (Key), 2);
  array_list_init (dual_consumer_keyset (keys, 1), sizeof (Key), 2);

  /* init key arrays for SHA1 producer and SHA1 consumer schemes */
  array_list_init (single_keyset (keys, 2), sizeof (Key), 2);
  array_list_init (single_keyset (keys, 3), sizeof (Key), 2);
  
  return keys;
}

void elvin_keys_free (Keys *keys)
{
  if (keys == EMPTY_KEYS || keys == NULL)
    return;
  
  free_keyset (dual_producer_keyset (keys, 1));
  free_keyset (dual_consumer_keyset (keys, 1));
  
  array_list_free (keyset_for_scheme (keys, 1));
  
  free_keyset (keyset_for_scheme (keys, 2));
  free_keyset (keyset_for_scheme (keys, 3));
}

void elvin_key_free (Key *key)
{
  if (key && key->data)
  {
    free (key->data);
    
    key->data = NULL;
  }
}

void free_keyset (ArrayList *keyset)
{
  size_t i = keyset->item_count;
  Key *key = keyset->items;
  
  for ( ; i > 0; i--, key++)
    elvin_key_free (key);
  
  array_list_free (keyset);
}

Key elvin_key_create_from_string (const char *str)
{
  size_t length = strlen (str);
  Key key;
  
  key.data = memdup (str, length);
  key.length = length;
  
  return key;
}

Key elvin_key_create_from_data (const uint8_t *data, size_t length)
{
  Key key;
  
  key.data = memdup (data, length);
  key.length = length;
  
  return key;  
}

Key elvin_key_create_public (Key private_key, KeyScheme scheme)
{
  return (*scheme->hash) (private_key);
}

bool elvin_key_equal (Key key1, Key key2)
{
  return key1.length == key2.length && 
         memcmp (key1.data, key2.data, key1.length) == 0;
}

bool keysets_equal (ArrayList *keyset1, ArrayList *keyset2)
{
  if (keyset1->item_count == keyset2->item_count)
  {
    size_t i;
    
    for (i = 0; i < keyset1->item_count; i++)
    {
      if (!elvin_key_equal (array_list_get (keyset1, i, Key), 
                            array_list_get (keyset2, i, Key)))
      {
        return false;
      }
    }
    
    return true;
  } else
  {
    return false;
  }
}

bool elvin_keys_equal (Keys *keys1, Keys *keys2)
{
  uint32_t id;
  KeyScheme *scheme = schemes;
  
  for (id = 1; id <= KEY_SCHEME_COUNT; id++, scheme++)
  {
    if ((*scheme)->type == DUAL)
    {
      if (!(keysets_equal 
             (dual_producer_keyset (keys1, id), 
              dual_producer_keyset (keys2, id)) &&
            keysets_equal 
              (dual_consumer_keyset (keys1, id), 
               dual_consumer_keyset (keys2, id))))
      {
        return false;
      }
    } else
    {
      if (!keysets_equal (single_keyset (keys1, id), 
                          single_keyset (keys2, id)))
      {
        return false;
      }
    }
  }
  
  return true;
}

bool elvin_keys_add (Keys *keys, KeyScheme scheme, Key key)
{
  /* TODO should check whether key already in list */
  
  if (scheme->type != DUAL)
  {
    *array_list_add (single_keyset (keys, scheme->id), Key) = key;
   
    return true;
  } else
  {
    return false;
  }
}

bool elvin_keys_add_dual_producer (Keys *keys, KeyScheme scheme, Key key)
{
  if (scheme->type == DUAL)
  {
    *array_list_add (dual_producer_keyset (keys, scheme->id), Key) = key;
   
    return true;
  } else
  {
    return false;
  }
}

bool elvin_keys_add_dual_consumer (Keys *keys, KeyScheme scheme, Key key)
{
  if (scheme->type == DUAL)
  {
    *array_list_add (dual_consumer_keyset (keys, scheme->id), Key) = key;
    
    return true;
  } else
  {
    return false;
  }
}

KeyScheme scheme_for (uint32_t id, ElvinError *error)
{
  if (id > 0 && id <= KEY_SCHEME_COUNT)
  {
    return schemes [id - 1];
  } else
  {
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, 
                     "Invalid key scheme ID: %u", id);
    
    return NULL;
  }
}

bool read_keyset (ByteBuffer *buffer, ArrayList *keyset, ElvinError *error)
{
  uint32_t key_count = byte_buffer_read_int32 (buffer, error);
  Array array;
  
  for ( ; key_count > 0 && elvin_error_ok (error); key_count--)
  {
    if (byte_buffer_read_byte_array (buffer, &array, error))
    {
      Key *key = array_list_add (keyset, Key);
      
      key->data = array.items;
      key->length = array.item_count;
    }
  }
  
  return elvin_error_ok (error); 
}

bool write_keyset (ByteBuffer *buffer, ArrayList *keyset, ElvinError *error)
{
  uint32_t key_count = keyset->item_count;
  Key *key = keyset->items;
  Array array;
  
  byte_buffer_write_int32 (buffer, key_count, error);
  
  for ( ; key_count > 0 && elvin_error_ok (error); key_count--, key++)
  {
    array.item_count = key->length;
    array.items = key->data;
    
    byte_buffer_write_byte_array (buffer, &array, error);
  }
  
  return elvin_error_ok (error);
}

/* TODO limit key counts and sizes */
bool elvin_keys_read (ByteBuffer *buffer, Keys *keys, ElvinError *error)
{
  uint32_t scheme_count = byte_buffer_read_int32 (buffer, error);
  
  for ( ; scheme_count > 0 && elvin_error_ok (error); scheme_count--)
  {
    uint32_t scheme_id = byte_buffer_read_int32 (buffer, error);
    uint32_t key_set_count = byte_buffer_read_int32 (buffer, error);
    KeyScheme scheme;
    
    if (elvin_error_occurred (error))
      break;
    
    on_error_return_false (scheme = scheme_for (scheme_id, error));
    
    if (scheme->type == DUAL)
    {
      if (key_set_count == 2)
      {
        read_keyset (buffer, dual_producer_keyset (keys, scheme_id), error) &&
        read_keyset (buffer, dual_consumer_keyset (keys, scheme_id), error);    
      } else
      {
        elvin_error_set (error, ELVIN_ERROR_PROTOCOL, 
                         "Dual key scheme without dual key sets");
      }
    } else
    {
      if (key_set_count == 1)
      {
        read_keyset (buffer, single_keyset (keys, scheme_id), error);
      } else
      {
        elvin_error_set (error, ELVIN_ERROR_PROTOCOL, 
                         "Single key scheme without single key set");
      }
    }
  }
  
  return elvin_error_ok (error);
}

bool elvin_keys_write (ByteBuffer *buffer, Keys *keys, ElvinError *error)
{
  int id;
  uint32_t scheme_count = 0;
  KeyScheme *scheme = schemes;
  ArrayList *list = keys->keys;
  size_t start = buffer->position;

  /* leave room for scheme count */
  byte_buffer_skip (buffer, 4, error);
  
  for (id = 1; id <= KEY_SCHEME_COUNT && elvin_error_ok (error); 
       id++, list++, scheme++)
  {
    if ((*scheme)->type == DUAL)
    {
      if (array_list_get (list, 0, ArrayList).item_count > 0 || 
          array_list_get (list, 1, ArrayList).item_count > 0)
      {
        byte_buffer_write_int32 (buffer, (*scheme)->id, error) &&
        byte_buffer_write_int32 (buffer, 2, error) &&
        write_keyset (buffer, &array_list_get (list, 0, ArrayList), error) &&
        write_keyset (buffer, &array_list_get (list, 1, ArrayList), error);
        
        scheme_count++;
      }
    } else
    {
      if (list->item_count > 0)
      {
        byte_buffer_write_int32 (buffer, (*scheme)->id, error) &&
        byte_buffer_write_int32 (buffer, 1, error) &&      
        write_keyset (buffer, list, error);
        
        scheme_count++;
      }
    }
  }
  
  if (elvin_error_ok (error))
  {
    /* write scheme count */
    size_t position = buffer->position;
    
    byte_buffer_set_position (buffer, start, error);
    byte_buffer_write_int32 (buffer, scheme_count, error);
    byte_buffer_set_position (buffer, position, error);
  }
  
  return elvin_error_ok (error);
}
