#include <avis/keys.h>

Keys _empty_keys = {0};

Keys *elvin_keys_init (Keys *keys)
{
  /* TODO */
  
  keys->dummy = 0;
  
  return keys;
}

void elvin_keys_free (Keys *keys)
{
  /* TODO */
}