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

START_TEST (test_connect)
{
  Elvin elvin;
  Elvin_Error error = elvin_error_create ();
    
  elvin_open (&elvin, "elvin://localhost", &error);
  fail_on_error (&error);
  
  elvin_close (&elvin);
}
END_TEST

TCase *client_tests ()
{
  TCase *tc_core = tcase_create ("client");
  tcase_add_test (tc_core, test_connect);
  
  return tc_core;
}