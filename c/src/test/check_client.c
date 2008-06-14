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

static void test_subscribe_sub_listener
  (Subscription *sub, Notification *notification, void *user_data);

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
    
  elvin_open (&elvin, "elvin://localhost", &error);
  fail_on_error (&error);
  
  elvin_close (&elvin);
}
END_TEST

START_TEST (test_notify)
{
  Elvin elvin;
    
  elvin_open (&elvin, "elvin://localhost", &error);
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
  time_t start_time;
  
  elvin_open (&elvin, "elvin://localhost", &error);
  fail_on_error (&error);

  /* check invalid subscription is handled */
  sub = elvin_subscribe (&elvin, "size (bogus", &error);
  
  fail_unless_error_code (&error, ELVIN_ERROR_SYNTAX);

  sub = elvin_subscribe (&elvin, "require (test) && string (message)", &error);
  fail_on_error (&error);
  
  elvin_subscription_add_listener (sub, test_subscribe_sub_listener, 
                                   "user_data");
  
  Attributes *ntfn = attributes_create ();
  
  attributes_set_int32 (ntfn, "test", 1);
  attributes_set_real64 (ntfn, "pi", M_PI);
  attributes_set_real64 (ntfn, "nan", NAN);
  attributes_set_string (ntfn, "message", "hello world");
  
  elvin_send (&elvin, ntfn, &error);
  fail_on_error (&error);
  
  attributes_destroy (ntfn);

  elvin_poll (&elvin, &error);
  fail_on_error (&error);
  
  /* Wait up to 3 seconds for notification to come in */
  start_time = time (NULL);
  
  while (!test_subscribe_received_ntfn &&
         difftime (time (NULL), start_time) < 3);
  {
    usleep (500);
  }
  
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
     "Invalid notification");
  
  fail_unless 
    (isnan (attributes_get_real64 (&notification->attributes, "nan")), 
     "Invalid notification");
  
  test_subscribe_received_ntfn = true;
}

TCase *client_tests ()
{
  TCase *tc_core = tcase_create ("client");
  tcase_add_test (tc_core, test_uri);
  tcase_add_test (tc_core, test_connect);
  tcase_add_test (tc_core, test_notify);
  tcase_add_test (tc_core, test_subscribe);
 
  tcase_add_checked_fixture (tc_core, setup, teardown);

  return tc_core;
}
