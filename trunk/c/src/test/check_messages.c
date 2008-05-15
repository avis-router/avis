#include <stdlib.h>
#include <check.h>

#include <elvin/elvin.h>
#include <elvin/errors.h>
#include <elvin/named_values.h>
#include <elvin/keys.h>

#include <messages.h>
#include "check_ext.h"

Suite *messages_suite (void);

START_TEST (test_fields)
{
  ConnRqst *connRqst = 
    ConnRqst_create (DEFAULT_CLIENT_PROTOCOL_MAJOR, DEFAULT_CLIENT_PROTOCOL_MINOR,
	                   EMPTY_NAMED_VALUES, EMPTY_KEYS, EMPTY_KEYS);

  Elvin_Error error = error_create ();
  Byte_Buffer *buffer = byte_buffer_create ();
  
  message_write (buffer, connRqst, &error);

  fail_on_error (&error);
  
  fail_unless (byte_buffer_len (buffer) == 12, "Size incorrect");
  
  ConnRqst *connRqst2;
  
  message_read (buffer, (void *)&connRqst2, &error);
  
  fail_on_error (&error);
  
  fail_unless (connRqst2->type == MESSAGE_CONN_RQST, "Type incorrect");
  
  byte_buffer_destroy (buffer);
}
END_TEST

Suite *messages_suite (void)
{
  Suite *s = suite_create ("Messages");

  /* Core test case */
  TCase *tc_core = tcase_create ("test_fields");
  // tcase_add_checked_fixture (tc_core, setup, teardown);
  tcase_add_test (tc_core, test_fields);
  suite_add_tcase (s, tc_core);

  return s;
}

int main (void)
{
  int number_failed;
  Suite *s = messages_suite ();
  SRunner *sr = srunner_create (s);
  srunner_run_all (sr, CK_NORMAL);
  number_failed = srunner_ntests_failed (sr);
  srunner_free (sr);
  return (number_failed == 0) ? EXIT_SUCCESS : EXIT_FAILURE;
}
