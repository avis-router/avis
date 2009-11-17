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
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <math.h>
#include <string.h>

#ifndef WIN32
  #include <unistd.h>
#endif

#include <check.h>

#include <avis/keys.h>
#include <avis/stdtypes.h>

#include "byte_buffer.h"
#include "keys_private.h"

#include "check_ext.h"

static ElvinError error = ELVIN_EMPTY_ERROR;

TCase *security_tests ();

static void setup ()
{
}

static void teardown ()
{
  elvin_error_free (&error);
}

START_TEST (test_keys)
{
  uint8_t data [4] = {1, 2, 3, 42};
  Key key1 = elvin_key_create_from_string ("key2");
  Key key2 = elvin_key_create_from_data (data, 4);
  Keys *keys1 = elvin_keys_create ();
  Keys *keys2 = elvin_keys_create ();

  fail_unless (elvin_keys_equal (keys1, keys2), "Keys not equal");

  elvin_keys_add (keys1, KEY_SCHEME_SHA1_CONSUMER, key1);
  elvin_keys_add (keys1, KEY_SCHEME_SHA1_PRODUCER, key2);

  fail_if (elvin_keys_equal (keys1, keys2), "Keys equal");

  elvin_keys_add (keys2, KEY_SCHEME_SHA1_CONSUMER, elvin_key_copy (key1));
  elvin_keys_add (keys2, KEY_SCHEME_SHA1_PRODUCER, elvin_key_copy (key2));

  fail_unless (elvin_keys_equal (keys1, keys2), "Keys not equal");

  elvin_keys_add_dual_consumer (keys1, KEY_SCHEME_SHA1_DUAL, elvin_key_copy (key1));
  elvin_keys_add_dual_producer (keys1, KEY_SCHEME_SHA1_DUAL, elvin_key_copy (key2));

  fail_if (elvin_keys_equal (keys1, keys2), "Keys equal");
  elvin_keys_add_dual_consumer (keys2, KEY_SCHEME_SHA1_DUAL, elvin_key_copy (key1));
  fail_if (elvin_keys_equal (keys1, keys2), "Keys equal");

  elvin_keys_add_dual_producer (keys2, KEY_SCHEME_SHA1_DUAL, elvin_key_copy (key2));
  fail_unless (elvin_keys_equal (keys1, keys2), "Keys not equal");

  elvin_keys_destroy (keys1);
  elvin_keys_destroy (keys2);
}
END_TEST

START_TEST (test_keys_delta)
{
  KeysDelta delta;
  Keys *keys1 = elvin_keys_create ();
  Keys *keys2 = elvin_keys_create ();  
  Key key1 = elvin_key_create_from_string ("key 1");
  Key key2 = elvin_key_create_from_string ("key 2");
  Key key3 = elvin_key_create_from_string ("key 3");
  Key key;

  elvin_keys_compute_delta (&delta, keys1, keys2);
  
  fail_unless (single_keyset (delta.add, SHA1_PRODUCER_ID)->item_count == 0, "Not empty");
  fail_unless (single_keyset (delta.add, SHA1_CONSUMER_ID)->item_count == 0, "Not empty");
  fail_unless (dual_producer_keyset (delta.add, SHA1_DUAL_ID)->item_count == 0, "Not empty");
  fail_unless (dual_consumer_keyset (delta.add, SHA1_DUAL_ID)->item_count == 0, "Not empty");
  
  elvin_keys_free_shallow (delta.add);
  elvin_keys_free_shallow (delta.del);

  /* add a single producer key */
  elvin_keys_add (keys2, KEY_SCHEME_SHA1_PRODUCER, key1);

  elvin_keys_compute_delta (&delta, keys1, keys2);
  
  fail_unless (single_keyset (delta.add, SHA1_PRODUCER_ID)->item_count == 1, "Not 1");
  key = key_at (single_keyset (delta.add, SHA1_PRODUCER_ID), 0);
  
  fail_unless (elvin_key_equal (key, key1), "Not equal");
  
  fail_unless (single_keyset (delta.add, SHA1_CONSUMER_ID)->item_count == 0, "Not empty");
  fail_unless (dual_producer_keyset (delta.add, SHA1_DUAL_ID)->item_count == 0, "Not empty");

  elvin_keys_free_shallow (delta.add);
  elvin_keys_free_shallow (delta.del);
  
  /* remove a single producer key */
  elvin_keys_add (keys1, KEY_SCHEME_SHA1_PRODUCER, key2);

  elvin_keys_compute_delta (&delta, keys1, keys2);
  
  fail_unless (single_keyset (delta.add, SHA1_PRODUCER_ID)->item_count == 1, "Not 1");
  key = key_at (single_keyset (delta.add, SHA1_PRODUCER_ID), 0);
  
  fail_unless (elvin_key_equal (key, key1), "Not equal");
  
  fail_unless (single_keyset (delta.del, SHA1_PRODUCER_ID)->item_count == 1, "Not 1");
  key = key_at (single_keyset (delta.del, SHA1_PRODUCER_ID), 0);
  
  fail_unless (elvin_key_equal (key, key2), "Not equal");
  
  elvin_keys_free_shallow (delta.add);
  elvin_keys_free_shallow (delta.del);
  
  /* compute with dual key set */
  
  elvin_keys_free (keys1);
  elvin_keys_free (keys2);
    
  keys1 = elvin_keys_create ();
  keys2 = elvin_keys_create (); 
  
  elvin_keys_add_dual_producer (keys1, KEY_SCHEME_SHA1_DUAL, elvin_key_copy (key1));
  elvin_keys_add_dual_consumer (keys1, KEY_SCHEME_SHA1_DUAL, elvin_key_copy (key2));
  elvin_keys_add_dual_consumer (keys1, KEY_SCHEME_SHA1_DUAL, elvin_key_copy (key3));

  elvin_keys_add_dual_producer (keys2, KEY_SCHEME_SHA1_DUAL, elvin_key_copy (key3));
  elvin_keys_add_dual_consumer (keys2, KEY_SCHEME_SHA1_DUAL, elvin_key_copy (key3));

  elvin_keys_compute_delta (&delta, keys1, keys2);
  fail_unless (dual_producer_keyset (delta.add, SHA1_DUAL_ID)->item_count == 1, "Not 1");
  fail_unless (dual_producer_keyset (delta.del, SHA1_DUAL_ID)->item_count == 1, "Not 1");

  fail_unless (dual_consumer_keyset (delta.add, SHA1_DUAL_ID)->item_count == 0, "Not 0");
  fail_unless (dual_consumer_keyset (delta.del, SHA1_DUAL_ID)->item_count == 1, "Not 1");

  elvin_keys_free_shallow (delta.add);
  elvin_keys_free_shallow (delta.del);
  
  elvin_keys_free (keys1);
  elvin_keys_free (keys2);
}
END_TEST

START_TEST (test_key_io)
{
  uint8_t data [4] = {1, 2, 3, 42};
  Key key1 = elvin_key_create_from_string ("key2");
  Key key2 = elvin_key_create_from_data (data, 4);
  Keys *keys1 = elvin_keys_create ();
  Keys *keys2 = elvin_keys_create ();
  ByteBuffer *buffer = byte_buffer_create ();

  /* empty keys */
  elvin_keys_write (buffer, EMPTY_KEYS, &error);
  fail_on_error (&error);

  fail_unless (buffer->position == 4, "Keys length incorrect: %u != 4",
               buffer->position);

  byte_buffer_set_position (buffer, 0, &error);

  elvin_keys_read (buffer, keys2, &error);
  fail_on_error (&error);

  fail_unless (elvin_keys_equal (EMPTY_KEYS, keys2), "Keys not equal");

  elvin_keys_destroy (keys2);
  keys2 = elvin_keys_create ();

  byte_buffer_set_position (buffer, 0, &error);

  /* populated key set */
  elvin_keys_add (keys1, KEY_SCHEME_SHA1_CONSUMER, key1);
  elvin_keys_add (keys1, KEY_SCHEME_SHA1_PRODUCER, key2);

  elvin_keys_add_dual_consumer
    (keys1, KEY_SCHEME_SHA1_DUAL, elvin_key_copy (key1));
  elvin_keys_add_dual_producer
    (keys1, KEY_SCHEME_SHA1_DUAL, elvin_key_copy (key2));

  elvin_keys_write (buffer, keys1, &error);
  fail_on_error (&error);

  byte_buffer_set_position (buffer, 0, &error);

  elvin_keys_read (buffer, keys2, &error);
  fail_on_error (&error);

  fail_unless (elvin_keys_equal (keys1, keys2), "Keys not equal");

  elvin_keys_destroy (keys1);
  elvin_keys_destroy (keys2);

  byte_buffer_destroy (buffer);
}
END_TEST

TCase *security_tests ()
{
  TCase *tc_core = tcase_create ("security");

  tcase_add_test (tc_core, test_keys);
  tcase_add_test (tc_core, test_keys_delta);
  tcase_add_test (tc_core, test_key_io);
  
  tcase_add_checked_fixture (tc_core, setup, teardown);

  return tc_core;
}
