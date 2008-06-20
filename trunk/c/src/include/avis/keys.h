/** \file
 * Secure messaging keys.
 */
#ifndef ELVIN_KEYS_H
#define ELVIN_KEYS_H

#include <string.h>

#include <avis/stdtypes.h>
#include <avis/arrays.h>

/**
 * An empty Keys collection.
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
 * Once in use, key collections should be treated as immutable
 * i.e. never be modified directly after construction.
 * <p>
 * See also section 7.4 of the client protocol spec.
 */
typedef struct
{
  ArrayList keys [3];
} Keys;

struct KeyScheme;

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
typedef struct KeyScheme * KeyScheme;

extern struct KeyScheme _KEY_SCHEME_SHA1_DUAL;
extern struct KeyScheme _KEY_SCHEME_SHA1_PRODUCER;
extern struct KeyScheme _KEY_SCHEME_SHA1_CONSUMER;

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

#define elvin_keys_create() \
  (elvin_keys_init (malloc (sizeof (Keys))))

#define elvin_keys_destroy(keys) \
  (elvin_keys_free (keys), free (keys), keys = NULL)

Keys *elvin_keys_init (Keys *keys);

void elvin_keys_free (Keys *keys);

bool elvin_keys_equal (Keys *keys1, Keys *keys2);

bool elvin_keys_add (Keys *keys, KeyScheme scheme, Key key);

bool elvin_keys_add_dual_consumer (Keys *keys, KeyScheme scheme, Key key);

bool elvin_keys_add_dual_producer (Keys *keys, KeyScheme scheme, Key key);

void key_free (Key *key);

#define elvin_key_copy(key) \
  (Key){memdup ((key).data, (key).length), (key).length}

#define elvin_key_create_from_string(str) \
  {(uint8_t *)strdup (str), strlen (str)}

#define elvin_key_create_from_data(data, length) {memdup (data, length), length}

Key elvin_public_key (Key private_key, KeyScheme scheme);

bool elvin_key_equal (Key *key1, Key *key2);

#endif /* ELVIN_KEYS_H */
