#include <stdlib.h>
#include <stdint.h>
#include <check.h>
#include <stdio.h>
#include <unistd.h>

#include <elvin/elvin.h>
#include <elvin/errors.h>
#include <elvin/named_values.h>
#include <elvin/keys.h>

#include <messages.h>
#include <byte_buffer.h>
#include "check_ext.h"

static ElvinError error = elvin_error_create ();

static void setup ()
{
}

static void teardown ()
{
  elvin_error_destroy (&error);
}

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
  named_values_set_string (ntfn, "string", "paydirt");
  
  elvin_send (&elvin, ntfn, &error);
  fail_on_error (&error);
  
  named_values_destroy (ntfn);
  
  elvin_close (&elvin);
}
END_TEST

TCase *client_tests ()
{
  TCase *tc_core = tcase_create ("client");
  tcase_add_test (tc_core, test_connect);
  tcase_add_test (tc_core, test_notify);
 
  tcase_add_checked_fixture (tc_core, setup, teardown);

  return tc_core;
}