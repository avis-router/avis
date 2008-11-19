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
#include <errno.h>
#include <string.h>
#include <assert.h>

/* ntohl () and htonl()  */
#ifdef _WIN32
  #include <winsock2.h>
#else
  #include <arpa/inet.h>
#endif

#include <avis/stdtypes.h>
#include <avis/defs.h>
#include <avis/errors.h>

#include "byte_buffer.h"
#include "avis_endian.h"
#include "errors_private.h"

#define INIT_LENGTH  1024
#define MAX_LENGTH   (10 * 1024 * 1024)

#define padding_for(length) ((4 - ((length) & 3)) & 3)

static bool check_remaining (ByteBuffer *buffer, size_t required_remaining,
                             ElvinError *error);

static void expand_buffer (ByteBuffer *buffer, size_t new_length);

static bool write_padding_for (ByteBuffer *buffer, uint32_t length,
                               ElvinError *error);

ByteBuffer *byte_buffer_init (ByteBuffer *buffer)
{
  buffer->data_length = INIT_LENGTH;
  buffer->max_data_length = MAX_LENGTH;
  buffer->data = emalloc (INIT_LENGTH);
  buffer->position = 0;

  return buffer;
}

ByteBuffer *byte_buffer_init_sized (ByteBuffer *buffer, size_t initial_size)
{
  buffer->data_length = initial_size;
  buffer->max_data_length = initial_size;
  buffer->data = emalloc (initial_size);
  buffer->position = 0;

  return buffer;
}

void byte_buffer_free (ByteBuffer *buffer)
{
  if (buffer->data)
  {
    free (buffer->data);

    buffer->data = NULL;
    buffer->data_length = 0;
  }
}

bool byte_buffer_set_position (ByteBuffer *buffer, size_t position,
                               ElvinError *error)
{
  if (buffer->position != position)
  {
    if (byte_buffer_ensure_capacity (buffer, position, error))
    {
      buffer->position = position;

      return true;
    } else
    {
      return false;
    }
  }

  return true;
}

bool byte_buffer_ensure_capacity (ByteBuffer *buffer, size_t min_length,
                                  ElvinError *error)
{
  if (buffer->data_length < min_length)
  {
    size_t new_length;

    if (min_length > buffer->max_data_length)
      return elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Buffer overflow");

    /* new length = min_length rounded up to nearest ^ 2 */
    /* TODO could do this more efficiently */
    new_length = buffer->data_length;

    while (new_length < min_length)
      new_length *= 2;

    expand_buffer (buffer, new_length);
  }

  return true;
}

void expand_buffer (ByteBuffer *buffer, size_t new_length)
{
  uint8_t *new_data;

  assert (new_length > buffer->data_length);

  new_data = realloc (buffer->data, new_length);

  if (new_data)
  {
    buffer->data = new_data;
    buffer->data_length = new_length;
  } else
  {
    error_fail ("Failed to resize byte buffer");
  }
}

bool check_remaining (ByteBuffer *buffer, size_t required_remaining,
                      ElvinError *error)
{
  if (buffer->data_length - buffer->position < required_remaining)
    return elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Buffer underflow");
  else
    return true;
}

int32_t byte_buffer_read_int32 (ByteBuffer *buffer, ElvinError *error)
{
  int32_t value;

  if (!check_remaining (buffer, 4, error))
    return 0;

  /* read and convert XDR -> native endianness */
  value = ntohl (*(int32_t *)(buffer->data + buffer->position));

  buffer->position += 4;

  return value;
}

bool byte_buffer_write_int32 (ByteBuffer *buffer, int32_t value,
                              ElvinError *error)
{
  on_error_return_false
    (byte_buffer_ensure_capacity (buffer, buffer->position + 4, error));

  *(int32_t *)(buffer->data + buffer->position) = htonl (value);

  buffer->position += 4;

  return true;
}

int64_t byte_buffer_read_int64 (ByteBuffer *buffer, ElvinError *error)
{
  uint8_t bytes [8];

  #if __BYTE_ORDER == __LITTLE_ENDIAN
    int i;
  #endif

  if (!check_remaining (buffer, 8, error))
    return 0;

  #if __BYTE_ORDER == __LITTLE_ENDIAN
    for (i = 0; i < 8; i++)
      bytes [7 - i] = buffer->data [buffer->position++];
  #else
    memcpy (bytes, buffer->data + buffer->position, 8);

    buffer->position += 8;
  #endif

  return *(int64_t *)bytes;
}

bool byte_buffer_write_int64 (ByteBuffer *buffer, int64_t value,
                              ElvinError *error)
{
  uint8_t *bytes = (uint8_t *)&value;

  #if __BYTE_ORDER == __LITTLE_ENDIAN
    int i;
  #endif

  if (!byte_buffer_ensure_capacity (buffer, buffer->position + 8, error))
    return false;

  #if __BYTE_ORDER == __LITTLE_ENDIAN
    for (i = 0; i < 8; i++)
      buffer->data [buffer->position++] = bytes [7 - i];
  #else
    memcpy (buffer->data + buffer->position, bytes, 8);

    buffer->position += 8;
  #endif

  return true;
}

real64_t byte_buffer_read_real64 (ByteBuffer *buffer, ElvinError *error)
{
  uint8_t bytes [8];

  #if __BYTE_ORDER == __LITTLE_ENDIAN
    int i;
  #endif

  if (!check_remaining (buffer, 8, error))
    return 0;

  #if __BYTE_ORDER == __LITTLE_ENDIAN
    for (i = 0; i < 8; i++)
      bytes [7 - i] = buffer->data [buffer->position++];
  #else
    memcpy (bytes, buffer->data + buffer->position, 8);

    buffer->position += 8;
  #endif

  return *(real64_t *)bytes;
}

bool byte_buffer_write_real64 (ByteBuffer *buffer, real64_t number,
                               ElvinError *error)
{
  uint8_t *bytes = (uint8_t *)&number;

  #if __BYTE_ORDER == __LITTLE_ENDIAN
    int i;
  #endif

  if (!byte_buffer_ensure_capacity (buffer, buffer->position + 8, error))
    return false;

  #if __BYTE_ORDER == __LITTLE_ENDIAN
    for (i = 0; i < 8; i++)
      buffer->data [buffer->position++] = bytes [7 - i];
  #else
    memcpy (buffer->data + buffer->position, bytes, 8);

    buffer->position += 8;
  #endif

  return true;
}

bool byte_buffer_read_bytes (ByteBuffer *buffer, uint8_t *bytes,
                             size_t bytes_len, ElvinError *error)
{
  on_error_return_false (check_remaining (buffer, bytes_len, error));

  memcpy (bytes, buffer->data + buffer->position, bytes_len);

  buffer->position += bytes_len;

  return true;
}

bool byte_buffer_write_bytes (ByteBuffer *buffer, uint8_t *bytes,
                              size_t bytes_len, ElvinError *error)
{
  on_error_return_false
    (byte_buffer_ensure_capacity (buffer, buffer->position + bytes_len, error));

  memcpy (buffer->data + buffer->position, bytes, bytes_len);

  buffer->position += bytes_len;

  return true;
}

bool byte_buffer_write_string (ByteBuffer *buffer, const char *string,
                               ElvinError *error)
{
  size_t length = strlen (string);

  check_max_size (length, MAX_STRING_LENGTH, "String too long", error);

  if (!elvin_error_ok (error))
    return false;

  return
    byte_buffer_ensure_capacity (buffer, buffer->position + length + 8, error) &&
    byte_buffer_write_int32 (buffer, (uint32_t)length, error) &&
    byte_buffer_write_bytes (buffer, (uint8_t *)string, length, error) &&
    write_padding_for (buffer, length, error);
}

char *byte_buffer_read_string (ByteBuffer *buffer, ElvinError *error)
{
  uint32_t length = byte_buffer_read_int32 (buffer, error);
  char *string;

  check_max_size (length, MAX_STRING_LENGTH, "String too long", error);

  if (!elvin_error_ok (error))
    return NULL;

  string = emalloc (length + 1);

  if (byte_buffer_read_bytes (buffer, (uint8_t *)string, length, error) &&
      byte_buffer_skip (buffer, padding_for (length), error))
  {
    string [length] = '\0';

    return string;
  } else
  {
    free (string);

    return NULL;
  }
}

bool byte_buffer_read_byte_array (ByteBuffer *buffer, Array *array,
                                  ElvinError *error)
{
  uint32_t length = byte_buffer_read_int32 (buffer, error);
  uint8_t *bytes;

  check_max_size (length, MAX_OPAQUE_LENGTH, "Opaque array too long", error);

  if (!elvin_error_ok (error))
    return false;

  bytes = emalloc (length);

  if (byte_buffer_read_bytes (buffer, bytes, length, error) &&
      byte_buffer_skip (buffer, padding_for (length), error))
  {
    array->items = bytes;
    array->item_count = length;

    return true;
  } else
  {
    free (bytes);

    return false;
  }
}

bool byte_buffer_write_byte_array (ByteBuffer *buffer, Array *array,
                                   ElvinError *error)
{
  return
    byte_buffer_write_int32 (buffer, array->item_count, error) &&
    byte_buffer_write_bytes (buffer, array->items, array->item_count, error) &&
    write_padding_for (buffer, array->item_count, error);
}

bool write_padding_for (ByteBuffer *buffer, uint32_t length, ElvinError *error)
{
  uint32_t padding = padding_for (length);

  if (padding > 0)
  {
    on_error_return_false
      (byte_buffer_ensure_capacity (buffer, buffer->position + padding, error));

    memset (buffer->data + buffer->position, 0, padding);

    buffer->position += padding;
  }

  return true;
}
