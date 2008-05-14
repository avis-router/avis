#include <stdlib.h>
#include <check.h>

#include <elvin/elvin.h>
#include <elvin/named_values.h>
#include <elvin/keys.h>

#include "messages.h"

Suite *messages_suite (void);

START_TEST (test_fields)
{
  Message *connRqst = 
    ConnRqst_create (DEFAULT_CLIENT_PROTOCOL_MAJOR, DEFAULT_CLIENT_PROTOCOL_MINOR,
                     EMPTY_NAMED_VALUES, EMPTY_KEYS, EMPTY_KEYS);

  int32_t version_major = connRqst->fields[0].value.value_int32;
  
  fail_unless (version_major == DEFAULT_CLIENT_PROTOCOL_MAJOR,
               "Version major incorrect");
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
