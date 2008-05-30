#include <stdlib.h>
#include <stdint.h>
#include <check.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include <elvin/elvin.h>
#include <elvin/errors.h>
#include <elvin/named_values.h>
#include <elvin/keys.h>

#include "messages.h"
#include "byte_buffer.h"

#include "named_values_private.h"

#include "check_ext.h"

static ElvinError error = elvin_error_create ();

static void setup ()
{
}

static void teardown ()
{
  elvin_error_destroy (&error);
}

/**
 * Basic byte buffer IO.
 */
START_TEST (test_byte_buffer_io)
{
  ByteBuffer *buffer = byte_buffer_create ();
  byte_buffer_set_max_length (buffer, 1024);
  
  byte_buffer_write_int32 (buffer, 42, &error);
  fail_on_error (&error);
  
  byte_buffer_set_position (buffer, 0, &error);
  fail_on_error (&error);
  
  uint32_t value;
  
  value = byte_buffer_read_int32 (buffer, &error);
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
  
  for (int i = 0; i < 1000; i++)
  {
    byte_buffer_write_int32 (buffer, i, &error);
    fail_on_error (&error);
  }
  
  byte_buffer_set_position (buffer, 0, &error);
    
  for (int i = 0; i < 1000; i++)
  {
    uint32_t value;
    value = byte_buffer_read_int32 (buffer, &error);
    fail_on_error (&error);
    fail_unless (value == i, "Value not the same");
  }
  
  byte_buffer_destroy (buffer);
}
END_TEST

/**
 * String IO.
 */
START_TEST (test_string_io)
{
  ByteBuffer *buffer = byte_buffer_create ();
  const char *string2;
  
  byte_buffer_write_string (buffer, "hello world", &error);
  fail_on_error (&error);
  
  fail_unless (buffer->position == 15, "Length incorrect");
  
  byte_buffer_set_position (buffer, 0, &error);
  
  string2 = byte_buffer_read_string (buffer, &error);
  fail_on_error (&error);
  
  fail_unless (strcmp (string2, "hello world") == 0, "Strings not equal");
  
  byte_buffer_destroy (buffer);
}
END_TEST

/**
 * Named values IO.
 */
START_TEST (test_named_values_io)
{
  ByteBuffer *buffer = byte_buffer_create ();
  
  NamedValues *values = named_values_create ();
  
  named_values_set_int32 (values, "int32", 42);
  named_values_set_string (values, "string", "hello world");
  
  fail_unless (named_values_get_int32 (values, "int32") == 42, 
               "Failed to set value: %u", 
               named_values_get_int32 (values, "int32"));
  
  fail_unless 
    (strcmp (named_values_get_string (values, "string"), "hello world") == 0, 
     "Failed to set string");
  
  named_values_write (buffer, values, &error);
  fail_on_error (&error);
  
  byte_buffer_set_position (buffer, 0, &error);
  NamedValues *values2 = named_values_create ();  
  
  named_values_read (buffer, values2, &error);
  fail_on_error (&error);
  
  fail_unless (named_values_get_int32 (values2, "int32") == 42, 
               "Failed to deserialize int32");
  fail_unless 
    (strcmp (named_values_get_string (values2, "string"), "hello world") == 0, 
        "Failed to deserialize string");

  named_values_destroy (values);
  named_values_destroy (values2);
  byte_buffer_destroy (buffer);
}
END_TEST

START_TEST (test_message_io)
{
  ByteBuffer *buffer = byte_buffer_create ();
  
  // write message out
  Message connRqst = message_alloca ();
  
  message_init (connRqst, MESSAGE_ID_CONN_RQST,
                DEFAULT_CLIENT_PROTOCOL_MAJOR, 
                DEFAULT_CLIENT_PROTOCOL_MINOR,
                EMPTY_NAMED_VALUES, EMPTY_KEYS, EMPTY_KEYS);

  message_write (buffer, connRqst, &error);
  
  fail_on_error (&error);
  
  fail_unless (byte_buffer_position (buffer) == 32, 
               "Message length incorrect");
  
  Message connRqst2;
  
  // read message back
  byte_buffer_set_position (buffer, 0, &error);
  uint32_t frame_size;
  
  frame_size = byte_buffer_read_int32 (buffer, &error);
  fail_on_error (&error);

  fail_unless (frame_size == 28, "Frame size not sent correctly");
  
  byte_buffer_set_max_length (buffer, frame_size + 4);
  byte_buffer_ensure_capacity (buffer, frame_size + 4);
    
  connRqst2 = message_read (buffer, &error);
  
  fail_on_error (&error);
  
  fail_unless (message_type_of (connRqst2) == MESSAGE_ID_CONN_RQST, 
               "Type incorrect");
  
  fail_unless (*(uint32_t *)(connRqst2 + 8) == DEFAULT_CLIENT_PROTOCOL_MAJOR,
               "Major version incorrect");  
  fail_unless (*(uint32_t *)(connRqst2 + 12) == DEFAULT_CLIENT_PROTOCOL_MINOR,
               "Minor version incorrect");
  
  byte_buffer_destroy (buffer);
  
  message_destroy (connRqst2);
}
END_TEST

TCase *messages_tests ()
{
  TCase *tc_core = tcase_create ("test_message_io");
  tcase_add_checked_fixture (tc_core, setup, teardown);
  tcase_add_test (tc_core, test_byte_buffer_io);
  tcase_add_test (tc_core, test_string_io);
  tcase_add_test (tc_core, test_named_values_io);
  tcase_add_test (tc_core, test_message_io);

  return tc_core;
}