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
/** \file
 * Secure messaging keys.
 */
#ifndef ELVIN_KEYS_H
#define ELVIN_KEYS_H

#include <string.h>

#include <avis/stdtypes.h>
#include <avis/arrays.h>

/**
 * A pointer to an immutable empty Keys collection.
 */
#define EMPTY_KEYS (&_empty_keys)

/**
 * A key value used to secure notifications. A key is simply an
 * immutable block of bytes.
 * <p>
 * Elvin defines two types of key, <em>private</em> (or <em>raw</em>)
 * keys, and <em>public</em> (or <em>prime</em>) keys. A public
 * key is a one-way hash (e.g. using SHA-1) of a private key. A
 * private key may be any random data, or simply a password. A private
 * key is defined to match a public key if the corresponding hash of
 * its data matches the public key's data, e.g. if
 * <code>sha1 (privateKey.data) == publicKey.data</code>.
 * <p>
 * Note that this is not a public key system in the RSA sense but
 * that, like RSA, public keys can be shared in the open without loss
 * of security.
 */
typedef struct
{
  uint8_t * data;
  uint32_t  length;
} Key;

/**
 * A key collection used to secure notifications. A key collection
 * contains zero or more mappings from a KeyScheme to the Keys registered
 * for that scheme.
 * <p>
 * See also section 7.4 of the client protocol spec.
 * <p>
 * <h3>Ownership</h3>
 *
 * Once added to a Keys collection, a Key's data is considered to be
 * owned by the key set and will be freed when elvin_keys_free() is
 * invoked. Thus, if you wish to add the same key more than once you
 * should elvin_key_copy() it first.
 * <p>
 * Similarly, once a Keys instance has been used in a successful call
 * to an Elvin client connection (e.g. elvin_open_with_keys() or
 * elvin_subscribe_with_keys()), the connection will take ownership of
 * that key collection and free it when appropriate.
 * <p>
 * Once in use, key collections should be treated as immutable
 * and never be modified directly.
 */
typedef struct
{
  ArrayList keys [3];
} Keys;

struct KeyScheme_t;

/**
 * Defines an Elvin security scheme. A key scheme
 * defines a mode of sending or receiving notifications securely.
 *
 * <h3>The Producer Scheme</h3>
 *
 * In the producer scheme, consumers of notifications ensure that a
 * notification producer is known to them. The producer uses the
 * private key, and consumers use the public key. If the producer
 * keeps its private key secure, consumers can be assured they are
 * receiving notifications from a trusted producer.
 *
 * <h3>The Consumer Scheme</h3>
 *
 * In the consumer scheme, producers of notifications ensure that a
 * notification consumer is known to them, i.e. the producer controls
 * who can receive its notifications. In this scheme -- the reverse of
 * the producer scheme -- the consumer uses the private key, and
 * producers use the public key. If the consumer keeps its private key
 * secure, then the producer can be assured that only the trusted
 * consumer can receive its notifications.
 *
 * <h3>The Dual Scheme</h3>
 *
 * The dual scheme combines both the producer and consumer schemes, so
 * that both ends can send and receive securely. Typically both ends
 * exchange public keys, and each end then emits notifications with
 * both its private key and the public key(s) of its intended
 * consumer(s) attached. Similarly, each end would subscribe using its
 * private key and the public key(s) of its intended producer(s).
 *
 * <h3>Supported Schemes</h3>
 *
 * Avis currently supports just the SHA-1 secure hash as defined in
 * version 4.0 of the Elvin protocol. As such, three schemes are
 * available: KEY_SCHEME_SHA1_DUAL, KEY_SCHEME_SHA1_CONSUMER and
 * KEY_SCHEME_SHA1_PRODUCER.
 */
typedef struct KeyScheme_t * KeyScheme;

extern struct KeyScheme_t _KEY_SCHEME_SHA1_DUAL;
extern struct KeyScheme_t _KEY_SCHEME_SHA1_PRODUCER;
extern struct KeyScheme_t _KEY_SCHEME_SHA1_CONSUMER;

/**
 * The SHA-1 dual key scheme.
 *
 * @see KeyScheme
 */
#define KEY_SCHEME_SHA1_DUAL (&_KEY_SCHEME_SHA1_DUAL)

/**
 * The SHA-1 producer key scheme.
 *
 * @see KeyScheme
 */
#define KEY_SCHEME_SHA1_PRODUCER (&_KEY_SCHEME_SHA1_PRODUCER)

/**
 * The SHA-1 consumer key scheme.
 *
 * @see KeyScheme
 */
#define KEY_SCHEME_SHA1_CONSUMER (&_KEY_SCHEME_SHA1_CONSUMER)

extern Keys _empty_keys;

/**
 * Create an empty keys collection.
 *
 * @see elvin_keys_free()
 * @see elvin_keys_destroy()
 */
#define elvin_keys_create() \
  (elvin_keys_init ((Keys *)avis_emalloc (sizeof (Keys))))

/**
 * Macro to destroy and NULL a keys collection. Handles NULL and EMPTY_KEYS
 * values.
 *
 * @see elvin_keys_free()
 */
#define elvin_keys_destroy(keys) \
  if ((keys) != EMPTY_KEYS && (keys) != NULL) \
  {\
    elvin_keys_free (keys); free (keys); \
  }\
  keys = NULL;\

/**
 * Initialise a keys collection to empty.
 *
 * @see elvin_keys_create()
 */
Keys *elvin_keys_init (Keys *keys);

/**
 * Copy a key collection.
 *
 * @param keys The keys to copy.
 *
 * @return An independent copy of the source key collection.
 *
 * @see elvin_keys_free()
 * @see elvin_key_copy()
 */
Keys *elvin_keys_copy (Keys *keys);

/**
 * Free any resources held by key collection. This includes any key data
 * blocks referenced.
 *
 * @see Keys
 * @see elvin_keys_init()
 */
void elvin_keys_free (Keys *keys);

/**
 * Test if two key collections are logically equal.
 */
bool elvin_keys_equal (Keys *keys1, Keys *keys2);

/**
 * Add a key to the collection in a given security scheme.
 *
 * @param keys The keys to add to.
 * @param scheme the security scheme to associate the key with.
 * @param key The key to add. The key becomes owned by the collection and
 * will be freed when the collection is.
 * @return True if the key was added, false if the collection was not modified
 * (the key was already in the collection).
 *
 * @see elvin_keys_add_dual_consumer()
 * @see elvin_keys_add_dual_producer()
 */
bool elvin_keys_add (Keys *keys, KeyScheme scheme, Key key);

/**
 * Add a key to the collection as a consumer key in a given dual key security
 * scheme.
 *
 * @param keys The keys to add to.
 * @param scheme the security scheme to associate the key with. This must
 * be a dual scheme (e.g. KEY_SCHEME_SHA1_DUAL).
 * @param key The key to add. The key becomes owned by the collection and
 * will be freed when the collection is.
 * @return True if the key was added, false if the collection was not modified
 * (the key was already in the collection, or the scheme is not a dual key
 * scheme).
 *
 * @see elvin_keys_add()
 * @see elvin_keys_add_dual_producer()
 */
bool elvin_keys_add_dual_consumer (Keys *keys, KeyScheme scheme, Key key);

/**
 * Add a key to the collection as a producer key in a given dual key security
 * scheme.
 *
 * @param keys The keys to add to.
 * @param scheme the security scheme to associate the key with. This must
 * be a dual scheme (e.g. KEY_SCHEME_SHA1_DUAL).
 * @param key The key to add. The key becomes owned by the collection and
 * will be freed when the collection is.
 * @return True if the key was added, false if the collection was not modified
 * (the key was already in the collection, or the scheme is not a dual key
 * scheme).
 *
 * @see elvin_keys_add()
 * @see elvin_keys_add_dual_consumer()
 */
bool elvin_keys_add_dual_producer (Keys *keys, KeyScheme scheme, Key key);

/**
 * Free the data block associated with a key.
 *
 * @see elvin_key_create_from_string()
 * @see elvin_key_create_from_data()
 */
void elvin_key_free (Key key);

/**
 * Copy a key.
 *
 * @see elvin_key_create_from_data()
 */
#define elvin_key_copy(key) \
  (elvin_key_create_from_data ((key).data, (key).length))

/**
 * Create a key from a character string.
 *
 * @param str The string to use as the data block.
 *
 * @see elvin_key_create_from_data()
 * @see elvin_key_free()
 */
Key elvin_key_create_from_string (const char *str);

/**
 * Create a key from a block of data.
 *
 * @param data The data block.
 * @param length The length of the data block.
 *
 * @see elvin_key_create_from_string()
 * @see elvin_key_create_public()
 * @see elvin_key_free()
 */
Key elvin_key_create_from_data (const uint8_t *data, size_t length);

/**
 * Create a public key from a private key using a given scheme's hash.
 *
 * @param private_key The private key block.
 * @param scheme The security scheme to use.
 *
 * @return The public key. public_key.data = hash (private_key.data)
 *
 * @see elvin_key_create_from_data()
 * @see elvin_key_free()
 */
Key elvin_key_create_public (Key private_key, KeyScheme scheme);

/**
 * Test if two keys are equal.
 */
bool elvin_key_equal (Key key1, Key key2);

#endif /* ELVIN_KEYS_H */
