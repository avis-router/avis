/** \file
 * Secure messaging keys.
 */
#ifndef ELVIN_KEYS_H
#define ELVIN_KEYS_H

#include <string.h>

#include <avis/stdtypes.h>
#include <avis/array_list.h>

#define EMPTY_KEYS (&_empty_keys)

typedef struct
{
  uint8_t * data;
  uint32_t  length;
} Key;

typedef struct
{
  ArrayList keys [3];
} Keys;

struct KeyScheme;

typedef struct KeyScheme * KeyScheme;

extern struct KeyScheme _KEY_SCHEME_SHA1_DUAL;
extern struct KeyScheme _KEY_SCHEME_SHA1_PRODUCER;
extern struct KeyScheme _KEY_SCHEME_SHA1_CONSUMER;

#define KEY_SCHEME_SHA1_DUAL (&_KEY_SCHEME_SHA1_DUAL)
#define KEY_SCHEME_SHA1_PRODUCER (&_KEY_SCHEME_SHA1_PRODUCER)
#define KEY_SCHEME_SHA1_CONSUMER (&_KEY_SCHEME_SHA1_CONSUMER)

extern Keys _empty_keys;

#define elvin_keys_create() \
  (elvin_keys_init (malloc (sizeof (Keys))))

#define elvin_keys_destroy(keys) \
  (elvin_keys_free (keys), free (keys), keys = NULL)

Keys *elvin_keys_init (Keys *keys);

void elvin_keys_free (Keys *keys);

bool elvin_keys_add (Keys *keys, KeyScheme scheme, Key key);

bool elvin_keys_add_dual_consumer (Keys *keys, KeyScheme scheme, Key key);

bool elvin_keys_add_dual_producer (Keys *keys, KeyScheme scheme, Key key);

#define elvin_key_from_string(str) \
  {(uint8_t *)strdup (str), strlen (str)}

#define elvin_key_from_data(data, length) {data, length}

#endif /* ELVIN_KEYS_H */
