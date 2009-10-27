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

#include "check.h"
#include <stdio.h>
#include <string.h>

#ifndef _WIN32
  #include <unistd.h>
#endif

#include <avis/elvin.h>
#include <avis/errors.h>
#include <avis/attributes.h>
#include <avis/keys.h>
#include <avis/stdtypes.h>

#include "messages.h"
#include "byte_buffer.h"

#include "attributes_private.h"
#include "errors_private.h"

#include "check_ext.h"

static ElvinError error = ELVIN_EMPTY_ERROR;

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
  uint32_t value;
  int64_t value64;
  real64_t value_real64;
  size_t bytes_len;
  uint8_t *bytes;
  uint8_t *read_bytes;
  int i;

  ByteBuffer *buffer = byte_buffer_create ();
  byte_buffer_set_max_length (buffer, 1024);

  byte_buffer_write_int32 (buffer, 42, &error);
  fail_on_error (&error);

  byte_buffer_set_position (buffer, 0, &error);
  fail_on_error (&error);

  value = byte_buffer_read_int32 (buffer, &error);
  fail_on_error (&error);

  fail_unless (value == 42, "Value incorrect: %u", value);

  /* test resize */
  byte_buffer_set_position (buffer, 0, &error);

  bytes_len = 20 * 1024;
  bytes = emalloc (bytes_len);

  for (i = 0; i < bytes_len; i++)
    bytes [i] = (uint8_t)i;

  /* try to write beyond max */
  byte_buffer_write_bytes (buffer, bytes, bytes_len, &error);
  fail_unless_error_code (&error, ELVIN_ERROR_PROTOCOL);

  /* exand max, retry */
  byte_buffer_set_max_length (buffer, bytes_len);
  byte_buffer_write_bytes (buffer, bytes, bytes_len, &error);
  fail_on_error (&error);

  read_bytes = emalloc (bytes_len);

  byte_buffer_set_position (buffer, 0, &error);
  byte_buffer_read_bytes (buffer, read_bytes, bytes_len, &error);
  fail_on_error (&error);

  for (i = 0; i < bytes_len; i++)
    fail_unless (bytes [i] == read_bytes [i], "Bytes differ at %u", i);

  free (bytes);
  free (read_bytes);

  /* read/write ints */
  byte_buffer_destroy (buffer);
  buffer = byte_buffer_create ();

  for (i = 0; i < 1000; i++)
  {
    byte_buffer_write_int32 (buffer, i, &error);
    fail_on_error (&error);
  }

  byte_buffer_set_position (buffer, 0, &error);

  for (i = 0; i < 1000; i++)
  {
    int32_t value;
    value = byte_buffer_read_int32 (buffer, &error);
    fail_on_error (&error);
    fail_unless (value == i, "Value not the same");
  }

  /* int64 */
  byte_buffer_set_position (buffer, 0, &error);
  byte_buffer_write_int64 (buffer, 123456790L, &error);
  fail_on_error (&error);

  byte_buffer_set_position (buffer, 0, &error);
  value64 = byte_buffer_read_int64 (buffer, &error);
  fail_on_error (&error);

  fail_unless (value64 == 123456790L, "Value not the same: %lu\n", value64);

  /* real64 */
  byte_buffer_set_position (buffer, 0, &error);
  byte_buffer_write_real64 (buffer, 3.1415, &error);
  fail_on_error (&error);

  byte_buffer_set_position (buffer, 0, &error);
  value_real64 = byte_buffer_read_real64 (buffer, &error);
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

  /* empty attributes */
  attributes_write (buffer, EMPTY_ATTRIBUTES, &error);
  fail_on_error (&error);

  byte_buffer_set_position (buffer, 0, &error);
  attributes2 = attributes_create ();
  attributes_read (buffer, attributes2, &error);
  fail_on_error (&error);

  fail_unless (attributes_size (attributes2) == 0, "Empty attributes failed");

  byte_buffer_set_position (buffer, 0, &error);
  attributes_destroy (attributes2);

  /* non empty attributes */
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
  uint32_t frame_size;
  alloc_message (connRqst);
  alloc_message (connRqst2);

  /* write message out */
  avis_message_init (connRqst, MESSAGE_ID_CONN_RQST,
                     (uint32_t)DEFAULT_CLIENT_PROTOCOL_MAJOR,
                     (uint32_t)DEFAULT_CLIENT_PROTOCOL_MINOR,
                     EMPTY_ATTRIBUTES, EMPTY_KEYS, EMPTY_KEYS);

  avis_message_write (buffer, connRqst, &error);

  fail_on_error (&error);

  frame_size = buffer->position;

  fail_unless (frame_size == 28, "Message length incorrect");

  /* read message back */
  byte_buffer_set_position (buffer, 0, &error);

  byte_buffer_set_max_length (buffer, frame_size);

  avis_message_read (buffer, connRqst2, &error);

  fail_on_error (&error);

  fail_unless (message_type_of (connRqst2) == MESSAGE_ID_CONN_RQST,
               "Type incorrect");

  fail_unless (*(uint32_t *)(connRqst2 + 8) == DEFAULT_CLIENT_PROTOCOL_MAJOR,
               "Major version incorrect");
  fail_unless (*(uint32_t *)(connRqst2 + 12) == DEFAULT_CLIENT_PROTOCOL_MINOR,
               "Minor version incorrect");

  byte_buffer_destroy (buffer);

  avis_message_free (connRqst2);
}
END_TEST

/**
 * Check we handle case where router sends dud messages.
 */
START_TEST (test_dud_router)
{
  ByteBuffer *buffer = byte_buffer_create ();
  Attributes *attributes = attributes_create ();
  uint32_t length;
  alloc_message (message1);
  alloc_message (message2);

  attributes_set_int32 (attributes, "int32", 42);
  attributes_set_int64 (attributes, "int64", 0xDEADBEEFF00DL);
  attributes_set_string (attributes, "string", "hello world");

  /* write message out */

  avis_message_init (message1, MESSAGE_ID_CONN_RQST,
                (uint32_t)DEFAULT_CLIENT_PROTOCOL_MAJOR,
                (uint32_t)DEFAULT_CLIENT_PROTOCOL_MINOR,
                attributes, EMPTY_KEYS, EMPTY_KEYS);

  avis_message_write (buffer, message1, &error);
  fail_on_error (&error);

  /* create an underflow */

  length = buffer->position;
  buffer->max_data_length = buffer->data_length = length - 1;

  byte_buffer_set_position (buffer, 0, &error);

  avis_message_read (buffer, message2, &error);

  fail_unless_error_code (&error, ELVIN_ERROR_PROTOCOL);
  /* message2 should be freed by avis_message_read () on error, but should be OK to
   * free again */
  avis_message_free (message2);

  /* create a corrupt message */
  buffer->max_data_length = buffer->data_length = length;
  byte_buffer_set_position (buffer, 0, &error);

  /* generate "error: Invalid value type: 4294967295" */
  ((uint32_t *)buffer->data) [8] = 0xFFFFFFFF;

  avis_message_read (buffer, message2, &error);

  fail_unless_error_code (&error, ELVIN_ERROR_PROTOCOL);
  avis_message_free (message2);

  byte_buffer_destroy (buffer);

  /* create a message that's too big: too many attributes */
  buffer = byte_buffer_create ();
  avis_message_write (buffer, message1, &error);

  byte_buffer_set_position (buffer, 16, &error);
  byte_buffer_write_int32 (buffer, 0xFFFFFFFF, &error);
  byte_buffer_set_position (buffer, 0, &error);

  avis_message_read (buffer, message2, &error);

  fail_unless_error_code (&error, ELVIN_ERROR_PROTOCOL);
  avis_message_free (message2);

  byte_buffer_destroy (buffer);

  /* not freeing messages1 since it refers to global singletons */
  attributes_destroy (attributes);
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
  tcase_add_test (tc_core, test_dud_router);

  return tc_core;
}
