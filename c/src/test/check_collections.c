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

#ifndef WIN32
  #include <unistd.h>
#endif

#include <check.h>

#include <avis/stdtypes.h>

#include "attributes_private.h"
#include "arrays_private.h"
#include "check_ext.h"

TCase *collections_tests ();

START_TEST (test_attributes)
{
  Attributes *ntfn = attributes_create ();
  Attributes *copy;
  
  attributes_set_int32 (ntfn, "int32", 32);
  attributes_set_int64 (ntfn, "int64", 64);
  attributes_set_real64 (ntfn, "real64", 3.1415);
  attributes_set_string (ntfn, "string", "hello world");

  fail_unless (attributes_get_int32 (ntfn, "int32") == 32,
               "int32 attribute: %lu != %lu",
               attributes_get_int32 (ntfn, "int32"), 32);

  fail_unless (attributes_get_int64 (ntfn, "int64") == 64L,
               "int64 attribute: %lu != %lu",
               attributes_get_int64 (ntfn, "int64"), 64);

  fail_unless (attributes_get_real64 (ntfn, "real64") == 3.1415,
               "real64 attribute: %f != %f",
               attributes_get_real64 (ntfn, "real64"), 3.1415);

  fail_unless
    (strcmp (attributes_get_string (ntfn, "string"), "hello world") == 0,
     "string attribute");

  fail_unless
    (attributes_get_string (ntfn, "nonexistent") == NULL,
     "nonexistent string attribute");

  copy = attributes_clone (ntfn);
  
  fail_unless 
    (attributes_get_int32 (copy, "int32") == 
     attributes_get_int32 (ntfn, "int32"),
     "int32 attribute copy failed");

  fail_unless 
    (attributes_get_real64 (copy, "real64") == 
     attributes_get_real64 (ntfn, "real64"),
     "real64 attribute copy failed");
  
  fail_unless
    (strcmp (attributes_get_string (copy, "string"), 
             attributes_get_string (ntfn, "string")) == 0,
     "string attribute copy");

  fail_if
    (attributes_get_string (copy, "string") ==
     attributes_get_string (ntfn, "string"),
   "string attributes not copied");
  
  attributes_destroy (copy);
  attributes_destroy (ntfn);
}
END_TEST

START_TEST (test_array_list)
{
  ArrayList *list = array_list_create (int, 5);
  int i;

  for (i = 0; i < 1000; i++)
    array_list_add_int (list, i);

  fail_unless (array_list_size (list) == 1000, "Not full");

  fail_unless (*array_list_find_int (list, 42) == 42, "Find failed");
  fail_unless (array_list_find_int (list, 123456) == NULL, "Find failed");

  for (i = 0; i < 1000; i++)
    fail_unless (array_list_get_int (list, i) == i, "Not equal");

  for (i = 0; i < 1000; i++)
  {
    array_list_remove (list, 0, int);

    if (i < 999)
    {
      fail_unless (array_list_get_int (list, 0) == i + 1,
                  "Not equal %u != %u", i + 1, array_list_get_int (list, 0));
    }
  }

  fail_unless (array_list_size (list) == 0, "Not empty");

  array_list_destroy (list);

  list = array_list_create (int, 5);

  array_list_add_ptr (list, "1");
  array_list_add_ptr (list, "2");
  array_list_add_ptr (list, "3");
  array_list_add_ptr (list, "foobar");

  fail_unless (strcmp (array_list_get_ptr (list, 0), "1") == 0, "Not equal");
  fail_unless (strcmp (array_list_get_ptr (list, 3), "foobar") == 0,
               "Not equal");

  array_list_destroy (list);
}
END_TEST

TCase *collections_tests ()
{
  TCase *tc_core = tcase_create ("collections");

  tcase_add_test (tc_core, test_attributes);
  tcase_add_test (tc_core, test_array_list);

  return tc_core;
}
