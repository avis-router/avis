/** \file
 * Secure messaging keys.
 */
#ifndef ELVIN_KEYS_H
#define ELVIN_KEYS_H

#define EMPTY_KEYS (&_empty_keys)

typedef struct
{
  int dummy;
} Keys;

extern Keys _empty_keys;

Keys * elvin_keys_init (Keys *keys);

void elvin_keys_free (Keys *keys);

#endif /* ELVIN_KEYS_H */
