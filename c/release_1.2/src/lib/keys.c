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

#include <assert.h>

#include <avis/errors.h>
#include <avis/keys.h>
#include <avis/defs.h>

#include "keys_private.h"
#include "arrays_private.h"
#include "errors_private.h"

struct KeyScheme_t _KEY_SCHEME_SHA1_DUAL =     {1, DUAL,     avis_sha1};
struct KeyScheme_t _KEY_SCHEME_SHA1_PRODUCER = {2, PRODUCER, avis_sha1};
struct KeyScheme_t _KEY_SCHEME_SHA1_CONSUMER = {3, CONSUMER, avis_sha1};

static KeyScheme schemes [KEY_SCHEME_COUNT] =
{
  &_KEY_SCHEME_SHA1_DUAL,
  &_KEY_SCHEME_SHA1_PRODUCER,
  &_KEY_SCHEME_SHA1_CONSUMER
};

static ArrayList _empty_dual_keys [2] = {{NULL, 0, 0}, {NULL, 0, 0}};

Keys _empty_keys = {{{&_empty_dual_keys, 0, 2}, {NULL, 0, 0}, {NULL, 0, 0}}};

static KeyScheme scheme_for (uint32_t id, ElvinError *error);

static void init_dual_keysets (Keys *keys);

static bool key_set_contains (const ArrayList *keyset, const Key *key);

static void key_set_subtract (ArrayList *target,
                              const ArrayList *keys1,
                              const ArrayList *keys2);

static void keysets_copy (ArrayList *target, ArrayList *source);

static void free_keyset (ArrayList *keyset);

static bool keysets_equal (const ArrayList *keyset1, const ArrayList *keyset2);

static bool write_keyset (ByteBuffer *buffer, ArrayList *keyset,
                          ElvinError *error);

static bool read_keyset (ByteBuffer *buffer, ArrayList *keyset,
                         ElvinError *error);


void init_dual_keysets (Keys *keys)
{
  /* init producer/consumer sub-lists for SHA1 dual scheme */
  array_list_init (&keys->keys [0], sizeof (ArrayList), 2);

  keys->keys [0].item_count = 2;
}

Keys *elvin_keys_init (Keys *keys)
{
  init_dual_keysets (keys);

  array_list_init (dual_producer_keyset (keys, 1), sizeof (Key), 2);
  array_list_init (dual_consumer_keyset (keys, 1), sizeof (Key), 2);

  /* init key arrays for SHA1 producer and SHA1 consumer schemes */
  array_list_init (single_keyset (keys, 2), sizeof (Key), 2);
  array_list_init (single_keyset (keys, 3), sizeof (Key), 2);

  return keys;
}

Keys *elvin_keys_copy (Keys *keys)
{
  int id;
  Keys *copy;
  KeyScheme *scheme = schemes;

  if (keys == EMPTY_KEYS)
    return EMPTY_KEYS;

  copy = emalloc (sizeof (Keys));

  init_dual_keysets (copy);

  for (id = 1; id <= KEY_SCHEME_COUNT; id++, scheme++)
  {
    if ((*scheme)->type == DUAL)
    {
      keysets_copy (dual_producer_keyset (copy, id),
                    dual_producer_keyset (keys, id));
      keysets_copy (dual_consumer_keyset (copy, id),
                    dual_consumer_keyset (keys, id));
    } else
    {
      keysets_copy (single_keyset (copy, id), single_keyset (keys, id));
    }
  }

  return copy;
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

void elvin_keys_free_shallow (Keys *keys)
{
  if (keys == EMPTY_KEYS || keys == NULL)
    return;
  
  array_list_free (dual_producer_keyset (keys, 1));
  array_list_free (dual_consumer_keyset (keys, 1));
  
  array_list_free (keyset_for_scheme (keys, 1));
  
  array_list_free (keyset_for_scheme (keys, 2));
  array_list_free (keyset_for_scheme (keys, 3));
}

void elvin_key_free (Key key)
{
  if (key.data)
  {
    free (key.data);

    key.data = NULL;
  }
}

void free_keyset (ArrayList *keyset)
{
  size_t i = keyset->item_count;
  Key *key = keyset->items;

  for ( ; i > 0; i--, key++)
    elvin_key_free (*key);

  array_list_free (keyset);
}

Key elvin_key_create_from_string (const char *str)
{
  size_t length = strlen (str);
  Key key;

  assert (length < UINT32_MAX);
  
  key.data = avis_memdup (str, length);
  key.length = (uint32_t)length;

  return key;
}

Key elvin_key_create_from_data (const uint8_t *data, size_t length)
{
  Key key;

  assert (length < UINT32_MAX);
  
  key.data = avis_memdup (data, length);
  key.length = (uint32_t)length;

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

bool keysets_equal (const ArrayList *keyset1, const ArrayList *keyset2)
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

void keysets_copy (ArrayList *target, ArrayList *source)
{
  size_t i;

  target->item_count = source->item_count;
  target->items_length = source->items_length;
  target->items = emalloc (target->items_length);

  for (i = 0; i < source->item_count; i++)
    ((Key *)target->items) [i] = elvin_key_copy (((Key *)source->items) [i]);
}

bool elvin_keys_equal (const Keys *keys1, const Keys *keys2)
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

uint32_t elvin_keys_count (const Keys *keys)
{
  uint32_t count = 0;
  uint32_t id;
  KeyScheme *scheme = schemes;

  for (id = 1; id <= KEY_SCHEME_COUNT; id++, scheme++)
  {
    if ((*scheme)->type == DUAL)
    {
      count += (uint32_t)dual_producer_keyset (keys, id)->item_count;
      count += (uint32_t)dual_consumer_keyset (keys, id)->item_count;
    } else
    {
      count += (uint32_t)single_keyset (keys, id)->item_count;
    }
  }

  return count;
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

void elvin_keys_compute_delta (KeysDelta *delta, 
                               const Keys *keys1, 
                               const Keys *keys2)
{
  int id;
  KeyScheme *scheme = schemes;
  
  delta->add = elvin_keys_create ();
  delta->del = elvin_keys_create ();
  
  for (id = 1; id <= KEY_SCHEME_COUNT; id++, scheme++)
  {
    if ((*scheme)->type == DUAL)
    {
      key_set_subtract (dual_producer_keyset (delta->add, id),
                        dual_producer_keyset (keys2, id),
                        dual_producer_keyset (keys1, id));
      
      key_set_subtract (dual_producer_keyset (delta->del, id),
                        dual_producer_keyset (keys1, id),
                        dual_producer_keyset (keys2, id));
      
      key_set_subtract (dual_consumer_keyset (delta->add, id),
                        dual_consumer_keyset (keys2, id),
                        dual_consumer_keyset (keys1, id));
      
      key_set_subtract (dual_consumer_keyset (delta->del, id),
                        dual_consumer_keyset (keys1, id),
                        dual_consumer_keyset (keys2, id));
    } else
    {
      key_set_subtract (single_keyset (delta->add, id),
                        single_keyset (keys2, id),
                        single_keyset (keys1, id));
      
      key_set_subtract (single_keyset (delta->del, id),
                        single_keyset (keys1, id),
                        single_keyset (keys2, id));
    }
  }   
}

void key_set_subtract (ArrayList *target,
                       const ArrayList *keys1,
                       const ArrayList *keys2)
{
  Key *key;
  size_t count = keys1->item_count;
  
  for (key = keys1->items; count > 0; count--, key++)
  {
    if (!key_set_contains (keys2, key))
      *array_list_add (target, Key) = *key;
  }
}

bool key_set_contains (const ArrayList *keyset, const Key *key)
{
  Key *otherkey;
  size_t count = keyset->item_count;
  
  for (otherkey = keyset->items; count > 0; count--, otherkey++)
  {
    if (elvin_key_equal (*key, *otherkey))
      return true;
  }
  
  return false;
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

  check_max_size (key_count, MAX_KEY_COUNT, "Too many keys", error);

  for ( ; key_count > 0 && elvin_error_ok (error); key_count--)
  {
    if (byte_buffer_read_byte_array (buffer, &array, error))
    {
      Key *key = array_list_add (keyset, Key);

      key->data = array.items;
      key->length = (uint32_t)array.item_count;
    }
  }

  return elvin_error_ok (error);
}

bool write_keyset (ByteBuffer *buffer, ArrayList *keyset, ElvinError *error)
{
  uint32_t key_count = (uint32_t)keyset->item_count;
  Key *key = keyset->items;
  Array array;

  assert (keyset->item_count < UINT32_MAX);
  
  byte_buffer_write_int32 (buffer, key_count, error);

  for ( ; key_count > 0 && elvin_error_ok (error); key_count--, key++)
  {
    array.item_count = key->length;
    array.items = key->data;

    byte_buffer_write_byte_array (buffer, &array, error);
  }

  return elvin_error_ok (error);
}

bool elvin_keys_read (ByteBuffer *buffer, Keys *keys, ElvinError *error)
{
  uint32_t scheme_count = byte_buffer_read_int32 (buffer, error);

  for ( ; scheme_count > 0 && elvin_error_ok (error); scheme_count--)
  {
    uint32_t scheme_id = byte_buffer_read_int32 (buffer, error);
    uint32_t key_set_count = byte_buffer_read_int32 (buffer, error);
    KeyScheme scheme;

    check_max_size
      (key_set_count, MAX_KEY_SCHEME_COUNT, "Too many key sets", error);

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
        if (!(byte_buffer_write_int32 (buffer, (*scheme)->id, error) &&
              byte_buffer_write_int32 (buffer, 2, error) &&
              write_keyset 
                (buffer, &array_list_get (list, 0, ArrayList), error) &&
              write_keyset 
                (buffer, &array_list_get (list, 1, ArrayList), error)))
        {
          return false;
        }

        scheme_count++;
      }
    } else
    {
      if (list->item_count > 0)
      {
        if (!(byte_buffer_write_int32 (buffer, (*scheme)->id, error) &&
              byte_buffer_write_int32 (buffer, 1, error) &&
              write_keyset (buffer, list, error)))
        {
          return false;
        }

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
