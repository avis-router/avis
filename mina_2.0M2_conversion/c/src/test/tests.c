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
#ifndef WIN32
#include <unistd.h>
#endif /* !WIN32 */

#include <check.h>

TCase *messages_tests ();
TCase *client_tests ();
TCase *collections_tests ();
TCase *security_tests ();
TCase *uri_tests ();

Suite *suite ()
{
  Suite *s = suite_create ("Avis Client");

  suite_add_tcase (s, uri_tests ());
  suite_add_tcase (s, messages_tests ());
  suite_add_tcase (s, client_tests ());
  suite_add_tcase (s, collections_tests ());
  suite_add_tcase (s, security_tests ());

  return s;
}

int main ()
{
  int number_failed;
  Suite *s = suite ();
  SRunner *sr = srunner_create (s);
  srunner_set_fork_status (sr, CK_FORK);
  srunner_run_all (sr, CK_NORMAL);
  number_failed = srunner_ntests_failed (sr);
  srunner_free (sr);

  return (number_failed == 0) ? EXIT_SUCCESS : EXIT_FAILURE;
}
