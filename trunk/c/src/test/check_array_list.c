#include <stdlib.h>
#include <stdint.h>
#include <check.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>

#include "array_list_private.h"
#include "check_ext.h"

TCase *array_list_tests ();

START_TEST (test_array_list)
{
  ArrayList *list = array_list_create (int, 5);
 
  for (int i = 0; i < 1000; i++)
    array_list_add_int (list, i);
  
  fail_unless (array_list_size (list) == 1000, "Not full");

  fail_unless (*array_list_find_int (list, 42) == 42, "Find failed");
  fail_unless (array_list_find_int (list, 123456) == NULL, "Find failed");
  
  for (int i = 0; i < 1000; i++)
    fail_unless (array_list_get_int (list, i) == i, "Not equal");
    
  for (int i = 0; i < 1000; i++)
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

TCase *array_list_tests ()
{
  TCase *tc_core = tcase_create ("array_list");
  
  tcase_add_test (tc_core, test_array_list);
  
  return tc_core;
}