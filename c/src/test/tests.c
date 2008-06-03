#include <stdlib.h>
#include <unistd.h>

#include <check.h>

TCase *messages_tests ();
TCase *client_tests ();

Suite *suite ()
{
  Suite *s = suite_create ("Avis Client");

  suite_add_tcase (s, messages_tests ());
  suite_add_tcase (s, client_tests ());
  suite_add_tcase (s, array_list_tests ());

  return s;
}

int main ()
{
  int number_failed;
  Suite *s = suite ();
  SRunner *sr = srunner_create (s);
  srunner_run_all (sr, CK_NORMAL);
  number_failed = srunner_ntests_failed (sr);
  srunner_free (sr);

  return (number_failed == 0) ? EXIT_SUCCESS : EXIT_FAILURE;
}
