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
#include <string.h>

#ifdef _WIN32
  #include <float.h>

  /* pull in M_PI */
  #define _USE_MATH_DEFINES
#else
  #include <unistd.h>
#endif

#include <math.h>

#include <check.h>

#include <avis/elvin.h>
#include <avis/errors.h>
#include <avis/stdtypes.h>
#include "errors_private.h"
#include "avis/attributes.h"
#include "avis/keys.h"

#include "messages.h"
#include "byte_buffer.h"
#include "check_ext.h"

static const char *elvin_router ();

static void close_listener
  (Elvin *elvin, CloseReason reason, const char *message, void *user_data);

static void test_subscribe_sub_listener
  (Subscription *sub, Attributes *attributes, bool secure, void *user_data);

static void test_subscribe_general_listener (Elvin *elvin,
                                             Attributes *attributes,
                                             bool secure,
                                             void *user_data);

static void check_secure_send_receive (Elvin *client, Subscription *secure_sub);

const char *elvin_router ()
{
  const char *uri = getenv ("ELVIN");

  return uri == NULL ? "elvin://localhost" : uri;
}

START_TEST (test_errors)
{
  ElvinError error = ELVIN_EMPTY_ERROR;

  elvin_error_set (&error, 666, "Test %i", 42);

  fail_unless (strcmp (error.message, "Test 42") == 0, "Message incorrect");

  elvin_error_free (&error);

  /* avis_fail ("Eeek! %u", __FILE__, __LINE__, 42); */
}
END_TEST

static bool close_listener_called = false;

START_TEST (test_connect)
{
  Elvin elvin;

  elvin_open (&elvin, elvin_router ());
  fail_on_error (&elvin.error);

  close_listener_called = false;

  elvin_add_close_listener (&elvin, close_listener, "user_data");

  elvin_close (&elvin);

  fail_unless (close_listener_called, "Close listener not called");

  /* test failed connect */
  elvin_open (&elvin, "elvin://bogus_host_name:1234");

  fail_if (elvin.error.code == ELVIN_ERROR_NONE,
           "Failed to handle missing router");

  elvin_close (&elvin);
}
END_TEST

void close_listener
  (Elvin *elvin, CloseReason reason, const char *message, void *user_data)
{
  fail_if (elvin_is_open (elvin), "Elvin should be marked as closed");
  fail_unless (strcmp (user_data, "user_data") == 0, "User data incorrect");
  fail_unless (reason == REASON_CLIENT_SHUTDOWN, "Reason incorrect");

  close_listener_called = true;
}

START_TEST (test_notify)
{
  Elvin elvin;
  Attributes *ntfn = attributes_create ();

  elvin_open (&elvin, elvin_router ());
  fail_on_error (&elvin.error);

  attributes_set_int32 (ntfn, "int32", 42);
  attributes_set_int64 (ntfn, "int64", 0xDEADBEEFL);
  attributes_set_string (ntfn, "string", "paydirt");

  elvin_send (&elvin, ntfn);
  fail_on_error (&elvin.error);

  attributes_destroy (ntfn);

  elvin_close (&elvin);
}
END_TEST

static bool test_subscribe_received_ntfn = false;

START_TEST (test_subscribe)
{
  Elvin elvin;
  Subscription *sub;
  Attributes *ntfn = attributes_create ();
  Array data;

  data.item_count = 100 * 1024;
  data.items = emalloc (data.item_count);

  memset (data.items, 42, data.item_count);

  elvin_open (&elvin, elvin_router ());
  fail_on_error (&elvin.error);

  /* check invalid subscription is handled */
  sub = elvin_subscribe (&elvin, "size (bogus");

  fail_unless_error_code (&elvin.error, ELVIN_ERROR_SYNTAX);

  sub =
    elvin_subscribe
      (&elvin,
       "require (test) && string (message) && int32 == 32 && int64 == 64L");
  fail_on_error (&elvin.error);

  elvin_subscription_add_listener (sub, test_subscribe_sub_listener,
                                   "user_data");

  attributes_set_int32 (ntfn, "test", 1);
  attributes_set_int32 (ntfn, "int32", 32);
  attributes_set_int64 (ntfn, "int64", 64);
  attributes_set_real64 (ntfn, "pi", M_PI);
  attributes_set_string (ntfn, "message", "hello world");
  attributes_set_opaque (ntfn, "opaque", data);

  test_subscribe_received_ntfn = false;
  elvin_send (&elvin, ntfn) && elvin_poll (&elvin);
  fail_on_error (&elvin.error);

  fail_unless (test_subscribe_received_ntfn, "Did not get notification");

  elvin_unsubscribe (&elvin, sub);
  fail_on_error (&elvin.error);

  /* test sub change*/
  sub = elvin_subscribe (&elvin, "require (bogus)");
  fail_on_error (&elvin.error);

  elvin_subscription_add_listener (sub, test_subscribe_sub_listener,
                                   "user_data");

  elvin_subscription_set_expr (sub, "require (test)");
  fail_on_error (&elvin.error);

  test_subscribe_received_ntfn = false;
  elvin_send (&elvin, ntfn) && elvin_poll (&elvin);
  fail_on_error (&elvin.error);

  fail_unless (test_subscribe_received_ntfn, "Did not get notification");

  attributes_destroy (ntfn);

  /* test listener removal */
  fail_unless
    (elvin_subscription_remove_listener (sub, test_subscribe_sub_listener),
     "Failed to remove sub listener");

  fail_if
    (elvin_subscription_remove_listener (sub, test_subscribe_sub_listener),
     "Failed to detect redundant remove of sub listener");

  /* test general listener */
  ntfn = attributes_create ();
  attributes_set_int32 (ntfn, "test", 1);

  elvin_add_notification_listener
    (&elvin, test_subscribe_general_listener, NULL);

  test_subscribe_received_ntfn = false;
  elvin_send (&elvin, ntfn) && elvin_poll (&elvin);
  fail_on_error (&elvin.error);

  fail_unless (test_subscribe_received_ntfn, "Did not get general notification");

  attributes_destroy (ntfn);

  elvin_unsubscribe (&elvin, sub);
  fail_on_error (&elvin.error);

  elvin_close (&elvin);
}
END_TEST

void test_subscribe_sub_listener (Subscription *sub,
                                  Attributes *attributes, bool secure,
                                  void *user_data)
{
  Array *data = attributes_get_opaque (attributes, "opaque");

  fail_unless (strcmp (user_data, "user_data") == 0, "User data incorrect");

  fail_unless
    (strcmp (attributes_get_string (attributes, "message"),
             "hello world") == 0, "Invalid notification");

  /* check the real64 made the roundtrip in case this system is not using
   * IEEE 754 for double precision floats. */
  fail_unless
    (attributes_get_real64 (attributes, "pi") == M_PI,
     "Invalid notification: PI != %f",
     attributes_get_real64 (attributes, "pi") );

  fail_unless (data != NULL, "Data missing");
  fail_unless (data->item_count == 100 * 1024, "Data wrong length");
  fail_unless (((uint8_t *)data->items) [10] == 42, "Data wrong");

  test_subscribe_received_ntfn = true;
}

void test_subscribe_general_listener (Elvin *elvin,
                                      Attributes *attributes, bool secure,
                                      void *user_data)
{
  test_subscribe_received_ntfn = true;
}

START_TEST (test_security)
{
  ElvinURI uri;
  Elvin alice_client;
  Elvin bob_client;
  Subscription *bob_sub;
  Key alice_private;
  Keys *alice_ntfn_keys;
  Keys *bob_sub_keys;
  ElvinError error = ELVIN_EMPTY_ERROR;

  elvin_uri_from_string (&uri, elvin_router (), &error);
  fail_on_error (&error);

  alice_private = elvin_key_create_from_string ("alice private");
  alice_ntfn_keys = elvin_keys_create ();
  elvin_keys_add (alice_ntfn_keys, KEY_SCHEME_SHA1_PRODUCER, alice_private);

  bob_sub_keys = elvin_keys_create ();
  elvin_keys_add
    (bob_sub_keys, KEY_SCHEME_SHA1_PRODUCER,
     elvin_key_create_public (alice_private, KEY_SCHEME_SHA1_PRODUCER));

  /* subscribe with global keys */

  elvin_open_with_keys (&alice_client, &uri,
                        elvin_keys_copy (alice_ntfn_keys), EMPTY_KEYS);
  fail_on_error (&alice_client.error);

  elvin_open_with_keys (&bob_client, &uri, EMPTY_KEYS,
                        elvin_keys_copy (bob_sub_keys));
  fail_on_error (&bob_client.error);

  bob_sub =
    elvin_subscribe_with_keys (&bob_client, "require (From-Alice)",
                               EMPTY_KEYS, REQUIRE_SECURE_DELIVERY);

  check_secure_send_receive (&alice_client, bob_sub);

  elvin_close (&alice_client);
  elvin_close (&bob_client);

  /* check we can add global keys later for same result */

  elvin_open_uri (&alice_client, &uri);
  fail_on_error (&alice_client.error);

  elvin_open_uri (&bob_client, &uri);
  fail_on_error (&bob_client.error);

  elvin_set_keys (&alice_client, elvin_keys_copy (alice_ntfn_keys),
                  EMPTY_KEYS);
  fail_on_error (&alice_client.error);

  elvin_set_keys (&bob_client, EMPTY_KEYS, elvin_keys_copy (bob_sub_keys));
  fail_on_error (&bob_client.error);

  bob_sub =
    elvin_subscribe_with_keys (&bob_client, "require (From-Alice)",
                               EMPTY_KEYS, REQUIRE_SECURE_DELIVERY);

  check_secure_send_receive (&alice_client, bob_sub);

  elvin_close (&alice_client);
  elvin_close (&bob_client);

  /* check we can add subscription keys for same result */
  elvin_open_with_keys (&alice_client, &uri,
                        elvin_keys_copy (alice_ntfn_keys), EMPTY_KEYS);
  fail_on_error (&alice_client.error);

  elvin_open_uri (&bob_client, &uri);
  fail_on_error (&bob_client.error);

  bob_sub = elvin_subscribe (&bob_client, "require (From-Alice)");

  elvin_subscription_set_keys (bob_sub, elvin_keys_copy (bob_sub_keys),
                               REQUIRE_SECURE_DELIVERY);
  fail_on_error (&bob_client.error);

  check_secure_send_receive (&alice_client, bob_sub);

  elvin_close (&alice_client);
  elvin_close (&bob_client);

  elvin_keys_destroy (alice_ntfn_keys);
  elvin_keys_destroy (bob_sub_keys);

  elvin_uri_free (&uri);
}
END_TEST

void test_security_sub_listener (Subscription *sub,
                                 Attributes *attributes,
                                 bool secure, void *user_data)
{
  fail_unless (secure, "Not secure");

  test_subscribe_received_ntfn = true;
}

void check_secure_send_receive (Elvin *client, Subscription *secure_sub)
{
  Attributes *ntfn = attributes_create ();

  elvin_subscription_add_listener
    (secure_sub, test_security_sub_listener, NULL);

  attributes_set_int32 (ntfn, "From-Alice", 1);

  test_subscribe_received_ntfn = false;

  elvin_send (client, ntfn);
  fail_on_error (&client->error);

  attributes_destroy (ntfn);

  elvin_poll (secure_sub->elvin);
  fail_on_error (&client->error);

  fail_unless (test_subscribe_received_ntfn, "Did not get notification");

  elvin_subscription_remove_listener (secure_sub, test_security_sub_listener);
}

TCase *client_tests ()
{
  TCase *tc_core = tcase_create ("client");
  tcase_add_test (tc_core, test_errors);
  tcase_add_test (tc_core, test_connect);
  tcase_add_test (tc_core, test_notify);
  tcase_add_test (tc_core, test_subscribe);
  tcase_add_test (tc_core, test_security);

  return tc_core;
}
