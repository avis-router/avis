#include <stdlib.h>
#include <stdint.h>
#include <check.h>
#include <stdio.h>
#include <unistd.h>
#include <time.h>

#include <elvin/elvin.h>
#include <elvin/errors.h>
#include <elvin/named_values.h>
#include <elvin/keys.h>

#include <messages.h>
#include <byte_buffer.h>
#include "check_ext.h"

static void test_subscribe_sub_listener (Subscription *sub, 
                                         Notification *notification);

static ElvinError error = elvin_error_create ();

static void setup ()
{
}

static void teardown ()
{
  elvin_error_destroy (&error);
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
  check_invalid_uri ("elvin://host:hello");  
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
  
  NamedValues *ntfn = named_values_create ();
  
  named_values_set_int32 (ntfn, "int32", 42);
  named_values_set_int64 (ntfn, "int64", 0xDEADBEEFF00DL);
  named_values_set_string (ntfn, "string", "paydirt");
  
  elvin_send (&elvin, ntfn, &error);
  fail_on_error (&error);
  
  named_values_destroy (ntfn);
  
  elvin_close (&elvin);
}
END_TEST

static bool test_subscribe_received_ntfn = false;

START_TEST (test_subscribe)
{
  Elvin elvin;
  Subscription sub;
  time_t start_time;
  
  elvin_open (&elvin, "elvin://localhost", &error);
  fail_on_error (&error);
  
  elvin_subscription_init (&sub, "require (test) && string (message)");
  fail_on_error (&error);
  
  elvin_subscribe (&elvin, &sub, &error);
  fail_on_error (&error);
  
  elvin_subscription_add_listener (&sub, test_subscribe_sub_listener);
  
  NamedValues *ntfn = named_values_create ();
  
  named_values_set_int32 (ntfn, "test", 1);
  named_values_set_string (ntfn, "message", "hello world");
  
  elvin_send (&elvin, ntfn, &error);
  fail_on_error (&error);
  
  named_values_destroy (ntfn);

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
  
  elvin_unsubscribe (&elvin, &sub, &error);
  fail_on_error (&error);
    
  elvin_close (&elvin);
}
END_TEST

void test_subscribe_sub_listener (Subscription *sub, 
                                  Notification *notification)
{
  fail_unless 
    (strcmp (named_values_get_string (&notification->attributes, "message"), 
             "hello world") == 0, "Invalid notification");
  
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