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
#ifndef KEYS_PRIVATE_H_
#define KEYS_PRIVATE_H_

#include <avis/errors.h>
#include <avis/keys.h>

#include "byte_buffer.h"

#define PRODUCER 1
#define CONSUMER 2
#define DUAL     (PRODUCER + CONSUMER)

#define KEY_SCHEME_COUNT 3

#define SHA1_DUAL_ID      1
#define SHA1_PRODUCER_ID  2
#define SHA1_CONSUMER_ID  3

typedef Key (*HashFunc) (Key key);

struct KeyScheme_t
{
  int id;
  int type;
  HashFunc hash;
};

typedef struct
{
  Keys *add;
  Keys *del;
} KeysDelta;

#define index_for(scheme) ((scheme)->id - 1)

/** The key set (actually an array list) for a key collection and scheme ID */
#define keyset_for_scheme(k, id) (&((k)->keys [(id) - 1]))

/** The single key list for a single key scheme. */
#define single_keyset(k, id) (keyset_for_scheme ((k), (id)))

/** The dual producer key list for a key collection & scheme ID. */
#define dual_producer_keyset(k, id) \
  ((ArrayList *)(keyset_for_scheme((k), (id)))->items)

/** The dual consumer key list for a key collection & scheme ID. */
#define dual_consumer_keyset(k, id) \
  (((ArrayList *)(keyset_for_scheme((k), (id)))->items) + 1)

/** The key at index in a keyset (ArrayList)/ */
#define key_at(keyset, index) (((Key *)(keyset)->items) [(index)])

bool elvin_keys_read (ByteBuffer *buffer, Keys *keys, ElvinError *error);

bool elvin_keys_write (ByteBuffer *buffer, Keys *keys, ElvinError *error);

/** Compute the delta needed to change one key collection into another. */
void elvin_keys_compute_delta (KeysDelta *delta, 
                               const Keys *keys1, const Keys *keys2);

void elvin_keys_free_shallow (Keys *keys);

Key avis_sha1 (Key input);

#endif /*KEYS_PRIVATE_H_*/
