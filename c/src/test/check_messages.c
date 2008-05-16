#include <stdlib.h>
#include <stdint.h>
#include <check.h>
#include <stdio.h>

#include <elvin/elvin.h>
#include <elvin/errors.h>
#include <elvin/named_values.h>
#include <elvin/keys.h>

#include <messages.h>
#include <byte_buffer.h>
#include "check_ext.h"

Suite *messages_suite (void);

START_TEST (test_xdr_io)
{
  Elvin_Error error = error_create ();
  Byte_Buffer *buffer = byte_buffer_create ();
  byte_buffer_set_max_length (buffer, 1024);
  
  byte_buffer_write_int32 (buffer, 42, &error);
  fail_on_error (&error);
  
  byte_buffer_set_position (buffer, 0, &error);
  fail_on_error (&error);
  
  uint32_t value;
  
  byte_buffer_read_int32 (buffer, &value, &error);
  fail_on_error (&error);
  
  fail_unless (value == 42, "Value incorrect: %u", value);
  
  // test resize
  byte_buffer_set_position (buffer, 0, &error);
  
  size_t bytes_len = 20 * 1024;
  uint8_t *bytes = malloc (bytes_len);
  
  for (int i = 0; i < bytes_len; i++)
    bytes [i] = (uint8_t)i;
  
  // try to write beyond max
  byte_buffer_write_bytes (buffer, bytes, bytes_len, &error);
  fail_unless_error_code (&error, ELVIN_ERROR_PROTOCOL);
  
  // exand max, retry
  byte_buffer_set_max_length (buffer, bytes_len);
  byte_buffer_write_bytes (buffer, bytes, bytes_len, &error);
  fail_on_error (&error);
    
  uint8_t *read_bytes = malloc (bytes_len);
  
  byte_buffer_set_position (buffer, 0, &error);
  byte_buffer_read_bytes (buffer, read_bytes, bytes_len, &error);
  fail_on_error (&error);
  
  for (int i = 0; i < bytes_len; i++)
    fail_unless (bytes [i] == read_bytes [i], "Bytes differ at %u", i);
  
  free (bytes);
  free (read_bytes);
  
  // read/write ints
  byte_buffer_set_position (buffer, 0, &error);
  
  for (int i = 0; i < 10; i++)
  {
    byte_buffer_write_int32 (buffer, i, &error);
    fail_on_error (&error);
  }
  
  byte_buffer_set_position (buffer, 0, &error);
    
  for (int i = 0; i < 10; i++)
  {
    uint32_t value;
    byte_buffer_read_int32 (buffer, &value, &error);
    fail_on_error (&error);
    fail_unless (value == i, "Value not the same");
  }
  
  byte_buffer_destroy (buffer);
}
END_TEST

START_TEST (test_message_io)
{
  ConnRqst *connRqst = 
    ConnRqst_create (DEFAULT_CLIENT_PROTOCOL_MAJOR, 
                     DEFAULT_CLIENT_PROTOCOL_MINOR,
	                   EMPTY_NAMED_VALUES, EMPTY_KEYS, EMPTY_KEYS);

  Elvin_Error error = error_create ();
  Byte_Buffer *buffer = byte_buffer_create ();
  
  message_write (buffer, connRqst, &error);

  fail_on_error (&error);
  
  fail_unless (byte_buffer_position (buffer) == 28, "Message length incorrect");
  
  ConnRqst *connRqst2;
  
  byte_buffer_set_position (buffer, 0, &error);
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
  TCase *tc_core = tcase_create ("test_message_io");
  // tcase_add_checked_fixture (tc_core, setup, teardown);
  tcase_add_test (tc_core, test_xdr_io);
  tcase_add_test (tc_core, test_message_io);
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
