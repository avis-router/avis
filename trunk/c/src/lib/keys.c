#include <avis/errors.h>
#include <avis/keys.h>

#include "keys_private.h"
#include "array_list_private.h"

uint8_t *avis_sha1 (uint8_t *input, uint32_t length);

typedef uint8_t * (*HashFunc) (uint8_t *data, uint32_t length);

#define PRODUCER 1
#define CONSUMER 2

struct KeyScheme
{
  int id;
  HashFunc hash;
  int type;
};

#define index_for(scheme) ((scheme)->id - 1)

struct KeyScheme _KEY_SCHEME_SHA1_DUAL = {1, avis_sha1, PRODUCER + CONSUMER};
struct KeyScheme _KEY_SCHEME_SHA1_PRODUCER = {2, avis_sha1, PRODUCER};
struct KeyScheme _KEY_SCHEME_SHA1_CONSUMER = {3, avis_sha1, CONSUMER};

Keys _empty_keys = {{{NULL, 0, 0}, {NULL, 0, 0}, {NULL, 0, 0}}};

Keys *elvin_keys_init (Keys *keys)
{
  array_list_init (&keys->keys [0], sizeof (ArrayList [2]), 2);
  
  /* init producer/consumer sub arrays for SHA1 dual scheme */
  array_list_init (&((ArrayList *)keys->keys [0].items) [0], sizeof (Key), 2);
  array_list_init (&((ArrayList *)keys->keys [0].items) [1], sizeof (Key), 2);

  /* init key arrays for SHA1 produer and consumer schemes */
  array_list_init (&(keys->keys [1]), sizeof (Key), 2);
  array_list_init (&(keys->keys [2]), sizeof (Key), 2);
  
  return keys;
}

void elvin_keys_free (Keys *keys)
{
  array_list_free (&((ArrayList *)keys->keys [0].items) [0]);
  array_list_free (&((ArrayList *)keys->keys [0].items) [1]);
  
  array_list_free (&keys->keys [0]);
  array_list_free (&keys->keys [1]);
  array_list_free (&keys->keys [2]);
}

bool elvin_keys_add (Keys *keys, KeyScheme scheme, Key key)
{
  /* TODO should check whether key already in list */
  
  if (scheme->type != PRODUCER + CONSUMER)
  {
    *array_list_add (&keys->keys [index_for (scheme)], Key) = key;
   
    return true;
  } else
  {
    return false;
  }
}

bool elvin_keys_add_dual_producer (Keys *keys, KeyScheme scheme, Key key)
{
  if (scheme->type == PRODUCER + CONSUMER)
  {
    ArrayList *list = &((ArrayList *)keys->keys [index_for (scheme)].items) [0];
    
    ((Key *)array_list_add (list, Key)) [0] = key;
   
    return true;
  } else
  {
    return false;
  }
}

bool elvin_keys_add_dual_consumer (Keys *keys, KeyScheme scheme, Key key)
{
  if (scheme->type == PRODUCER + CONSUMER)
  {
    ArrayList *list = &((ArrayList *)keys->keys [index_for (scheme)].items) [0];
        
    ((Key *)array_list_add (list, Key)) [1] = key;
    
    return true;
  } else
  {
    return false;
  }
}

bool elvin_keys_read (ByteBuffer *buffer, Keys *keys, ElvinError *error)
{
  /* TODO */
  abort ();
  
  return false;
}

bool elvin_keys_write (ByteBuffer *buffer, Keys *keys, ElvinError *error)
{
  /* TODO */
  abort ();
  
  return false;
}