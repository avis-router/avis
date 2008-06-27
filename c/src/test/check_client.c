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

#ifdef WIN32
  #include <float.h>

  /* pull in M_PI */
  #define _USE_MATH_DEFINES

  /* Windows doesn't have a NaN definition (why?) */
  #ifndef NAN
    static const unsigned __int32 nan [2] = {0xffffffff, 0x7fffffff};
    #define NAN (*(const double *) nan)
  #endif

  #define isnan _isnan
#else
  #include <stdint.h>
  #include <unistd.h>
#endif

#include <math.h>

#include <check.h>

#include "avis/elvin.h"
#include "avis/errors.h"
#include "avis/attributes.h"
#include "avis/keys.h"

#include "messages.h"
#include "byte_buffer.h"
#include "check_ext.h"

static const char *elvin_router ();

static void test_subscribe_sub_listener
  (Subscription *sub, Attributes *attributes, bool secure, void *user_data);

static void check_secure_send_receive (Elvin *client, Subscription *secure_sub);

static ElvinError error = elvin_error_create ();

static void setup ()
{
}

static void teardown ()
{
  elvin_error_free (&error);
}

#define check_invalid_uri(uri_string)\
{\
  elvin_uri_from_string (&uri, uri_string, &error);\
  fail_unless_error_code (&error, ELVIN_ERROR_INVALID_URI);\
  reset_uri (uri);\
}

#define reset_uri(uri) (elvin_uri_free (&uri), uri.host = NULL, uri.port = 0)

const char *elvin_router ()
{
  const char *uri = getenv ("ELVIN");
  
  return uri == NULL ? "elvin://localhost" : uri;
}

START_TEST (test_uri)
{
  ElvinURI uri;
  
  elvin_uri_from_string (&uri, "elvin://host", &error);
  fail_on_error (&error);
  
  fail_unless (strcmp ("host", uri.host) == 0, "Bad host: %s", uri.host);
  
  reset_uri (uri);
  elvin_uri_from_string (&uri, "elvin://host:1234", &error);
  fail_on_error (&error);
  
  fail_unless (strcmp ("host", uri.host) == 0, "Bad host: %s", uri.host);
  fail_unless (uri.port == 1234, "Bad port: %s", uri.port);
  
  reset_uri (uri);
  elvin_uri_from_string 
    (&uri, "elvin:4.1/xdr,none,ssl/host:4567?name1=value1;name2=value2", 
     &error);
  fail_on_error (&error);

  fail_unless (strcmp ("host", uri.host) == 0, "Bad host: %s", uri.host);
  fail_unless (uri.port == 4567, "Bad port: %s", uri.port);
  
  reset_uri (uri);
  
  check_invalid_uri ("hello://host");  
  check_invalid_uri ("elvin://host:1234567890");  
  check_invalid_uri ("elvin://host:-1");  
  check_invalid_uri ("elvin://host:hello");  
  check_invalid_uri ("elvin://:1234");  
  check_invalid_uri ("elvin://");
  check_invalid_uri ("elvin:/");
  check_invalid_uri ("elvin::/");
  check_invalid_uri ("elvin:");
  check_invalid_uri ("elvin");
  check_invalid_uri ("");
}
END_TEST

START_TEST (test_connect)
{
  Elvin elvin;
    
  elvin_open (&elvin, elvin_router (), &error);
  fail_on_error (&error);
  
  elvin_close (&elvin);
}
END_TEST

START_TEST (test_notify)
{
  Elvin elvin;
  Attributes *ntfn = attributes_create ();
    
  elvin_open (&elvin, elvin_router (), &error);
  fail_on_error (&error);
  
  attributes_set_int32 (ntfn, "int32", 42);
  attributes_set_int64 (ntfn, "int64", 0xDEADBEEFL);
  attributes_set_string (ntfn, "string", "paydirt");
  
  elvin_send (&elvin, ntfn, &error);
  fail_on_error (&error);
  
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
  
  elvin_open (&elvin, elvin_router (), &error);
  fail_on_error (&error);

  /* check invalid subscription is handled */
  sub = elvin_subscribe (&elvin, "size (bogus", &error);
  
  fail_unless_error_code (&error, ELVIN_ERROR_SYNTAX);

  sub = 
    elvin_subscribe
      (&elvin, 
       "require (test) && string (message) && int32 == 32 && int64 == 64L", 
       &error);
  fail_on_error (&error);
  
  elvin_subscription_add_listener (sub, test_subscribe_sub_listener, 
                                   "user_data");
  
  attributes_set_int32 (ntfn, "test", 1);
  attributes_set_int32 (ntfn, "int32", 32);
  attributes_set_int64 (ntfn, "int64", 64);
  attributes_set_real64 (ntfn, "pi", M_PI);
  attributes_set_real64 (ntfn, "nan", NAN);
  attributes_set_string (ntfn, "message", "hello world");

  test_subscribe_received_ntfn = false;
  elvin_send (&elvin, ntfn, &error);
  fail_on_error (&error);
  
  attributes_destroy (ntfn);

  elvin_poll (&elvin, &error);
  fail_on_error (&error);
  
  fail_unless (test_subscribe_received_ntfn, "Did not get notification");
  
  /* test listener removal */
  fail_unless 
    (elvin_subscription_remove_listener (sub, test_subscribe_sub_listener), 
     "Failed to remove sub listener");
  
  fail_if
    (elvin_subscription_remove_listener (sub, test_subscribe_sub_listener), 
     "Failed to detect redundant remove of sub listener");
  
  elvin_unsubscribe (&elvin, sub, &error);
  fail_on_error (&error);
    
  elvin_close (&elvin);
}
END_TEST

void test_subscribe_sub_listener (Subscription *sub, 
                                  Attributes *attributes, bool secure, 
                                  void *user_data)
{
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

  
  fail_unless 
    (isnan (attributes_get_real64 (attributes, "nan")), 
     "Invalid notification: NaN != %f", 
     attributes_get_real64 (attributes, "nan"));
  
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
  
  elvin_uri_from_string (&uri, elvin_router (), &error);
  fail_on_error (&error);
  
  alice_private = elvin_key_create_from_string ("alice private");
  alice_ntfn_keys = elvin_keys_create ();
  elvin_keys_add (alice_ntfn_keys, KEY_SCHEME_SHA1_PRODUCER, alice_private);
  
  bob_sub_keys = elvin_keys_create ();
  elvin_keys_add 
    (bob_sub_keys, KEY_SCHEME_SHA1_PRODUCER, 
     elvin_key_create_public (alice_private, KEY_SCHEME_SHA1_PRODUCER));
  
  elvin_open_with_keys (&alice_client, &uri, 
                        alice_ntfn_keys, EMPTY_KEYS, &error);
  fail_on_error (&error);
    
  elvin_open_with_keys (&bob_client, &uri, EMPTY_KEYS, bob_sub_keys, &error);
  fail_on_error (&error);
  
  bob_sub = 
    elvin_subscribe_with_keys (&bob_client, "require (From-Alice)",
                               EMPTY_KEYS, REQUIRE_SECURE_DELIVERY, &error);
  
  check_secure_send_receive (&alice_client, bob_sub);
  
  elvin_close (&alice_client);
  elvin_close (&bob_client);
  
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
  
  elvin_send (client, ntfn, &error);
  fail_on_error (&error);
  
  attributes_destroy (ntfn);
  
  elvin_poll (secure_sub->elvin, &error);
  fail_on_error (&error);
    
  fail_unless (test_subscribe_received_ntfn, "Did not get notification");
  
  elvin_subscription_remove_listener (secure_sub, test_security_sub_listener);  
}
/*
 *   public void security ()
    throws Exception
  {
    createServer ();
    
    ElvinURI uri = new ElvinURI (ELVIN_URI);
    
    Key alicePrivate = new Key ("alice private");

    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_PRODUCER, alicePrivate);
    
    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_PRODUCER, alicePrivate.publicKeyFor (SHA1_PRODUCER));
    
    // subscribe with global keys
    Elvin aliceClient = new Elvin (uri, aliceNtfnKeys, EMPTY_KEYS);
    Elvin bobClient = new Elvin (uri, EMPTY_KEYS, bobSubKeys);
    Elvin eveClient = new Elvin (uri);
    
    Subscription bobSub =
      bobClient.subscribe ("require (From-Alice)", REQUIRE_SECURE_DELIVERY);
    
    Subscription eveSub = eveClient.subscribe ("require (From-Alice)");
    
    checkSecureSendReceive (aliceClient, bobSub, eveSub);
    
    aliceClient.close ();
    bobClient.close ();
    eveClient.close ();
    
    // check we can add global keys later for same result
    aliceClient = new Elvin (uri);
    bobClient = new Elvin (uri);
    eveClient = new Elvin (uri);
    
    aliceClient.setNotificationKeys (aliceNtfnKeys);
    
    bobSub = bobClient.subscribe ("require (From-Alice)",
                                  REQUIRE_SECURE_DELIVERY);
    bobClient.setSubscriptionKeys (bobSubKeys);
    
    eveSub = eveClient.subscribe ("require (From-Alice)");
    
    checkSecureSendReceive (aliceClient, bobSub, eveSub);
    
    aliceClient.close ();
    bobClient.close ();
    eveClient.close ();
    
    // check we can add subscription keys for same result
    aliceClient = new Elvin (uri, aliceNtfnKeys, EMPTY_KEYS);
    bobClient = new Elvin (uri);
    eveClient = new Elvin (uri);
    
    bobSub = bobClient.subscribe ("require (From-Alice)",
                                  REQUIRE_SECURE_DELIVERY);
    
    bobSub.setKeys (bobSubKeys);
    
    eveSub = eveClient.subscribe ("require (From-Alice)");
    
    checkSecureSendReceive (aliceClient, bobSub, eveSub);
    
    aliceClient.close ();
    bobClient.close ();
    eveClient.close ();
    
    // check we can subscribe securely in one step
    aliceClient = new Elvin (uri, aliceNtfnKeys, EMPTY_KEYS);
    bobClient = new Elvin (uri);
    eveClient = new Elvin (uri);
    
    bobSub = bobClient.subscribe ("require (From-Alice)",
                                  bobSubKeys, REQUIRE_SECURE_DELIVERY);
    
    eveSub = eveClient.subscribe ("require (From-Alice)");
    
    checkSecureSendReceive (aliceClient, bobSub, eveSub);
    
    aliceClient.close ();
    bobClient.close ();
    eveClient.close ();
  }
  */

TCase *client_tests ()
{
  TCase *tc_core = tcase_create ("client");
  tcase_add_test (tc_core, test_uri);
  tcase_add_test (tc_core, test_connect);
  tcase_add_test (tc_core, test_notify);
  tcase_add_test (tc_core, test_subscribe);
  tcase_add_test (tc_core, test_security);
 
  tcase_add_checked_fixture (tc_core, setup, teardown);

  return tc_core;
}
