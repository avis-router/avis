#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <unistd.h>
#include <time.h>
#include <math.h>
#include <string.h>

#include <check.h>

#include <avis/elvin.h>
#include <avis/errors.h>
#include <avis/attributes.h>
#include <avis/keys.h>

#include <messages.h>
#include <byte_buffer.h>
#include "check_ext.h"

static const char *elvin_router ();

static void test_subscribe_sub_listener
  (Subscription *sub, Notification *notification, void *user_data);

static void check_secure_send_receive (Elvin *client, Subscription *secure_sub);

static void wait_for_notification ();

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
}

#define reset_uri(uri) (uri.host = NULL, uri.port = 0)

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
    
  elvin_open (&elvin, elvin_router (), &error);
  fail_on_error (&error);
  
  Attributes *ntfn = attributes_create ();
  
  attributes_set_int32 (ntfn, "int32", 42);
  attributes_set_int64 (ntfn, "int64", 0xDEADBEEFF00DL);
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
  
  Attributes *ntfn = attributes_create ();
  
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
  
  wait_for_notification ();
  
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

/* TODO change to passing notification as args */
/* TODO think about max key size */
void test_subscribe_sub_listener (Subscription *sub, 
                                  Notification *notification, void *user_data)
{
  fail_unless (strcmp (user_data, "user_data") == 0, "User data incorrect");
  
  fail_unless 
    (strcmp (attributes_get_string (&notification->attributes, "message"), 
             "hello world") == 0, "Invalid notification");
  
  /* check the real64 made the roundtrip in case this system is not using 
   * IEEE 754 for double precision floats. */
  fail_unless
      (attributes_get_real64 (&notification->attributes, "pi") == M_PI,
       "Invalid notification: PI != %f",
       attributes_get_real64 (&notification->attributes, "pi") );

  
  fail_unless 
    (isnan (attributes_get_real64 (&notification->attributes, "nan")), 
     "Invalid notification: NaN != %f", 
     attributes_get_real64 (&notification->attributes, "nan"));
  
  test_subscribe_received_ntfn = true;
}

/*
 * TODO elvin connection should free keys. how to handle empty keys?
 */
START_TEST (test_security)
{
  ElvinURI uri;
  Subscription *bob_sub;
  Elvin alice_client;
  Elvin bob_client;
  
  elvin_uri_from_string (&uri, elvin_router (), &error);
  fail_on_error (&error);
  
  Key alice_private = elvin_key_from_string ("alice private");
  Keys *alice_ntfn_keys = elvin_keys_create ();
  elvin_keys_add (alice_ntfn_keys, KEY_SCHEME_SHA1_PRODUCER, alice_private);
  
  Keys* bob_sub_keys = elvin_keys_create ();
  elvin_keys_add (bob_sub_keys, KEY_SCHEME_SHA1_PRODUCER, 
                  elvin_public_key (&alice_private, KEY_SCHEME_SHA1_PRODUCER));
  
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
                                 Notification *notification, void *user_data)
{
  fail_unless (notification->secure, "Not secure");
  
  test_subscribe_received_ntfn = true;
}

void check_secure_send_receive (Elvin *client, Subscription *secure_sub)
{
  elvin_subscription_add_listener 
    (secure_sub, test_security_sub_listener, NULL);
  
  Attributes *ntfn = attributes_create ();
  attributes_set_int32 (ntfn, "From-Alice", 1);
  
  test_subscribe_received_ntfn = false;
  
  elvin_send (client, ntfn, &error);
  fail_on_error (&error);
  
  attributes_destroy (ntfn);
  
  elvin_poll (secure_sub->elvin, &error);
  fail_on_error (&error);
    
  wait_for_notification ();
  
  fail_unless (test_subscribe_received_ntfn, "Did not get notification");
  
  elvin_subscription_remove_listener (secure_sub, test_security_sub_listener);
  
//  TestNtfnListener insecureListener = new TestNtfnListener (insecureSub);
//  insecureListener.waitForSecureNotification (client, ntfn);
//  assertNull (insecureListener.event);
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

void wait_for_notification ()
{
  /* Wait up to 3 seconds for notification to come in */
  time_t start_time = time (NULL);
  
  while (!test_subscribe_received_ntfn &&
         difftime (time (NULL), start_time) < 3);
  {
    usleep (500);
  }
}

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
