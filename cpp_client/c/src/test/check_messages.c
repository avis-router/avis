#include <stdlib.h>
#include <stdint.h>
#include <check.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include <avis/elvin.h>
#include <avis/errors.h>
#include <avis/attributes.h>
#include <avis/keys.h>
#include <avis/stdtypes.h>

#include "messages.h"
#include "byte_buffer.h"

#include "attributes_private.h"

#include "check_ext.h"

static ElvinError error = elvin_error_create ();

static void setup ()
{
}

static void teardown ()
{
  elvin_error_free (&error);
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
  byte_buffer_destroy (buffer);
  buffer = byte_buffer_create ();
  
  for (int i = 0; i < 1000; i++)
  {
    byte_buffer_write_int32 (buffer, i, &error);
    fail_on_error (&error);
  }
  
  byte_buffer_set_position (buffer, 0, &error);
    
  for (int i = 0; i < 1000; i++)
  {
    int32_t value;
    value = byte_buffer_read_int32 (buffer, &error);
    fail_on_error (&error);
    fail_unless (value == i, "Value not the same");
  }
  
  // int64
  byte_buffer_set_position (buffer, 0, &error);
  byte_buffer_write_int64 (buffer, 123456790L, &error);
  fail_on_error (&error);
  
  byte_buffer_set_position (buffer, 0, &error);
  int64_t value64 = byte_buffer_read_int64 (buffer, &error);
  fail_on_error (&error);

  fail_unless (value64 == 123456790L, "Value not the same: %lu\n", value64);

  // real64
  byte_buffer_set_position (buffer, 0, &error);
  byte_buffer_write_real64 (buffer, 3.1415, &error);
  fail_on_error (&error);
  
  byte_buffer_set_position (buffer, 0, &error);
  real64_t value_real64 = byte_buffer_read_real64 (buffer, &error);
  fail_on_error (&error);

  fail_unless (value_real64 == 3.1415, 
               "Value not the same: %d\n", value_real64);

  byte_buffer_destroy (buffer);
}
END_TEST

/**
 * String IO.
 */
START_TEST (test_string_io)
{
  ByteBuffer *buffer = byte_buffer_create ();
  char *string2;
  
  byte_buffer_write_string (buffer, "hello world", &error);
  fail_on_error (&error);
  
  fail_unless (buffer->position == 16, "Length incorrect");
  
  byte_buffer_set_position (buffer, 0, &error);
  
  string2 = byte_buffer_read_string (buffer, &error);
  fail_on_error (&error);
  
  fail_unless (strcmp (string2, "hello world") == 0, "Strings not equal");
  
  byte_buffer_destroy (buffer);
  
  free (string2);
}
END_TEST

/**
 * Named attributes IO.
 */
START_TEST (test_attributes_io)
{
  ByteBuffer *buffer = byte_buffer_create ();
  Attributes *attributes2;
  Attributes *attributes;
  Array some_bytes;
  
  array_init (&some_bytes, 128, 1);
  
  memset (some_bytes.items, 42, 128);
  
  // empty attributes
  attributes_write (buffer, EMPTY_NAMED_VALUES, &error);
  fail_on_error (&error);
    
  byte_buffer_set_position (buffer, 0, &error);
  attributes2 = attributes_create ();
  attributes_read (buffer, attributes2, &error);
  fail_on_error (&error);
  
  fail_unless (attributes_size (attributes2) == 0, "Empty attributes failed");
  
  byte_buffer_set_position (buffer, 0, &error);
  attributes_destroy (attributes2);
  
  // non empty attributes
  attributes = attributes_create ();
  
  attributes_set_int32 (attributes, "int32", 42);
  attributes_set_int64 (attributes, "int64", 0xDEADBEEFF00DL);
  attributes_set_opaque (attributes, "opaque", some_bytes);
  attributes_set_string (attributes, "string", "hello world");
  
  attributes_write (buffer, attributes, &error);
  fail_on_error (&error);
  
  byte_buffer_set_position (buffer, 0, &error);
  attributes2 = attributes_create ();  
  
  attributes_read (buffer, attributes2, &error);
  fail_on_error (&error);
  
  fail_unless (attributes_get_int32 (attributes2, "int32") == 42, 
               "Failed to serialize value: %u", 
               attributes_get_int32 (attributes, "int32"));
  
  fail_unless (attributes_get_int64 (attributes2, "int64") == 0xDEADBEEFF00DL, 
               "Failed to serialize value: %lu", 
               attributes_get_int64 (attributes2, "int64"));

  fail_unless (array_equals (attributes_get_opaque (attributes2, "opaque"), 
               &some_bytes), "Failed to serialize opaque");
  
  fail_unless 
    (strcmp (attributes_get_string (attributes2, "string"), "hello world") == 0, 
     "Failed to serialize string");

  attributes_destroy (attributes);
  attributes_destroy (attributes2);
  byte_buffer_destroy (buffer);
}
END_TEST

START_TEST (test_message_io)
{
  ByteBuffer *buffer = byte_buffer_create ();
  
  // write message out
  alloc_message (connRqst);
  
  message_init (connRqst, MESSAGE_ID_CONN_RQST,
                DEFAULT_CLIENT_PROTOCOL_MAJOR, 
                DEFAULT_CLIENT_PROTOCOL_MINOR,
                EMPTY_NAMED_VALUES, EMPTY_KEYS, EMPTY_KEYS);

  message_write (buffer, connRqst, &error);
  
  fail_on_error (&error);
  
  fail_unless (buffer->position == 32, "Message length incorrect");
  
  alloc_message (connRqst2);
  
  // read message back
  byte_buffer_set_position (buffer, 0, &error);
  uint32_t frame_size;
  
  frame_size = byte_buffer_read_int32 (buffer, &error);
  fail_on_error (&error);

  fail_unless (frame_size == 28, "Frame size not sent correctly");
  
  byte_buffer_set_max_length (buffer, frame_size + 4);
  
  message_read (buffer, connRqst2, &error);
  
  fail_on_error (&error);
  
  fail_unless (message_type_of (connRqst2) == MESSAGE_ID_CONN_RQST, 
               "Type incorrect");
  
  fail_unless (*(uint32_t *)(connRqst2 + 8) == DEFAULT_CLIENT_PROTOCOL_MAJOR,
               "Major version incorrect");  
  fail_unless (*(uint32_t *)(connRqst2 + 12) == DEFAULT_CLIENT_PROTOCOL_MINOR,
               "Minor version incorrect");
  
  byte_buffer_destroy (buffer);
  
  message_free (connRqst2);
}
END_TEST

TCase *messages_tests ()
{
  TCase *tc_core = tcase_create ("test_message_io");
  tcase_add_checked_fixture (tc_core, setup, teardown);
  tcase_add_test (tc_core, test_byte_buffer_io);
  tcase_add_test (tc_core, test_string_io);
  tcase_add_test (tc_core, test_attributes_io);
  tcase_add_test (tc_core, test_message_io);

  return tc_core;
}
