#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <assert.h>

/* For definition of ntohl */
#ifdef WIN32
  #include <winsock2.h>
#endif

#include <elvin/stdtypes.h>
#include <elvin/errors.h>

#include "byte_buffer.h"

#define INIT_LENGTH 1024
#define MAX_LENGTH (2 * 1024 * 1024)

#define padding_for(length) ((4 - ((length) & 3)) & 3)

static bool check_remaining (ByteBuffer *buffer, size_t required_remaining, 
                             ElvinError *error);

static bool auto_resize_to_fit (ByteBuffer *buffer, size_t min_length,
                                ElvinError *error);

static void expand_buffer (ByteBuffer *buffer, size_t new_length);

static bool write_padding_for (ByteBuffer *buffer, uint32_t length, 
                               ElvinError *error);

ByteBuffer *byte_buffer_init (ByteBuffer *buffer)
{
  buffer->data_length = INIT_LENGTH;
  buffer->max_data_length = MAX_LENGTH;
  buffer->data = malloc (INIT_LENGTH);
  buffer->position = 0;
  
  return buffer;
}

ByteBuffer *byte_buffer_init_sized (ByteBuffer *buffer, size_t initial_size)
{
  buffer->data_length = initial_size;
  buffer->max_data_length = initial_size;
  buffer->data = malloc (initial_size);
  buffer->position = 0;
  
  return buffer;
}

void byte_buffer_free (ByteBuffer *buffer)
{
  if (buffer->data)
  {
    free (buffer->data);
    
    buffer->data = NULL;
    buffer->data_length = -1;
  }
}

bool byte_buffer_set_position (ByteBuffer *buffer, size_t position,
                               ElvinError *error)
{
  if (buffer->position != position)
  {
    on_error_return_false (auto_resize_to_fit (buffer, position, error));
    
    buffer->position = position;
  }
  
  return true;
}

bool auto_resize_to_fit (ByteBuffer *buffer, size_t min_length,
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
  uint8_t *new_data = malloc (new_length);
  uint8_t *old_data = buffer->data;
  
  assert (new_length > buffer->data_length);
  
  memcpy ((void *)new_data, (void *)old_data, buffer->data_length);
  
  /* TODO this may not be necessary: does malloc guarantee zeroed memory? */
  memset ((void *)(new_data + buffer->data_length), 0, 
          new_length - buffer->data_length);
  
  buffer->data = new_data;
  buffer->data_length = new_length;
  
  free (old_data);
}

void byte_buffer_ensure_capacity (ByteBuffer *buffer, size_t capacity)
{
  if (buffer->data_length < capacity)
    expand_buffer (buffer, capacity);
}

bool check_remaining (ByteBuffer *buffer, size_t required_remaining, 
                      ElvinError *error)
{
  if (buffer->data_length - buffer->position < required_remaining)
    return elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Buffer underflow");
  else
    return true;
}

uint32_t byte_buffer_read_int32 (ByteBuffer *buffer, ElvinError *error)
{
  uint32_t value;
  
  if (!check_remaining (buffer, 4, error))
    return 0;
  
  /* read and convert XDR -> native endianness */
  value = ntohl (*(uint32_t *)(buffer->data + buffer->position));
  
  buffer->position += 4;
  
  return value;
}

bool byte_buffer_write_int32 (ByteBuffer *buffer, uint32_t value, 
                              ElvinError *error)
{
  on_error_return_false 
    (auto_resize_to_fit (buffer, buffer->position + 4, error));
  
  *(uint32_t *)(buffer->data + buffer->position) = htonl (value); 
  
  buffer->position += 4;
  
  return true;
}

bool byte_buffer_write_string (ByteBuffer *buffer, const char *string, 
                               ElvinError *error)
{
  uint32_t length = (uint32_t)strlen (string);
  
  on_error_return_false 
    (auto_resize_to_fit (buffer, buffer->position + length + 8, error));
  
  on_error_return_false 
    (byte_buffer_write_int32 (buffer, length, error));
  
  on_error_return_false 
    (byte_buffer_write_bytes (buffer, (uint8_t *)string, length, error));
  
  return write_padding_for (buffer, length, error);
}

char *byte_buffer_read_string (ByteBuffer *buffer, ElvinError *error)
{
  uint32_t length;
  char *string;
  
  on_error_return (length = byte_buffer_read_int32 (buffer, error), NULL);
  
  string = malloc (length + 1);
  
  if (string == NULL)
  {
    elvin_error_set (error, ERRNO_TO_ELVIN_ERROR (errno), 
                     "Not enough memory to allocate string");
    
    return NULL;
  }
  
  if (!(byte_buffer_read_bytes (buffer, (uint8_t *)string, length, error) &&
        byte_buffer_skip (buffer, padding_for (length), error)))
  {
    free (string);
    
    return NULL;
  }
  
  string [length] = '\0';
  
  return string;
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
    (auto_resize_to_fit (buffer, buffer->position + bytes_len, error));
  
  memcpy (buffer->data + buffer->position, bytes, bytes_len);
  
  buffer->position += bytes_len;
  
  return true;
}

bool write_padding_for (ByteBuffer *buffer, uint32_t length, ElvinError *error)
{
  uint32_t padding = padding_for (length);

  if (padding > 0)
  {
    on_error_return_false 
      (byte_buffer_ensure_capacity (buffer, buffer->position + padding));
    
    memset (buffer->data + buffer->position, 0, padding);
    
    buffer->position += padding;
  }
  
  return true;
}
