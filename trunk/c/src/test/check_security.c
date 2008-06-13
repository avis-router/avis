#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <unistd.h>
#include <time.h>
#include <math.h>
#include <string.h>

#include <check.h>

#include <avis/keys.h>

#include <byte_buffer.h>
#include <keys_private.h>

#include "check_ext.h"

static ElvinError error = elvin_error_create ();

static void setup ()
{
}

static void teardown ()
{
  elvin_error_free (&error);
}

START_TEST (test_key_io)
{
  uint8_t data [4] = {1, 2, 3, 42};
  Key key1 = elvin_key_from_string ("key2");
  Key key2 = elvin_key_from_data (data, 4);
  Keys *keys = elvin_keys_create ();
  Keys *keys2 = elvin_keys_create ();
  ByteBuffer *buffer = byte_buffer_create ();
  
  elvin_keys_add (keys, KEY_SCHEME_SHA1_CONSUMER, key1);
  elvin_keys_add (keys, KEY_SCHEME_SHA1_PRODUCER, key2);
  
  elvin_keys_add_dual_consumer (keys, KEY_SCHEME_SHA1_DUAL, key1);
  elvin_keys_add_dual_producer (keys, KEY_SCHEME_SHA1_DUAL, key2);
  
  elvin_keys_write (buffer, keys, &error);
  fail_on_error (&error);

  byte_buffer_set_position (buffer, 0, &error);
  elvin_keys_read (buffer, keys2, &error);
  fail_on_error (&error);
  
  elvin_keys_destroy (keys); 
  elvin_keys_destroy (keys2); 
}
END_TEST

TCase *security_tests ()
{
  TCase *tc_core = tcase_create ("security");
  
  tcase_add_test (tc_core, test_key_io);
  
  tcase_add_checked_fixture (tc_core, setup, teardown);

  return tc_core;
}