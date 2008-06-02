/*
 *  keys.h
 *  Elvin Client
 *
 *  Created by Matthew Phillips on 13/05/08.
 *  Copyright 2008 __MyCompanyName__. All rights reserved.
 *
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

#endif /* ELVIN_KEYS_H */
