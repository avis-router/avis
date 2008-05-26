#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <netinet/in.h>

#include <elvin/errors.h>

#include "byte_buffer.h"

#define INIT_LENGTH 1024
#define MAX_LENGTH (2 * 1024 * 1024)

static bool check_remaining (ByteBuffer *buffer, size_t required_remaining, 
                             ElvinError *error);

static bool auto_resize_to_fit (ByteBuffer *buffer, size_t min_length,
                                ElvinError *error);

static void expand_buffer (ByteBuffer *buffer, size_t new_length);

ByteBuffer *byte_buffer_create ()
{
  ByteBuffer *buffer = malloc (sizeof (ByteBuffer));
  
  buffer->data_length = INIT_LENGTH;
  buffer->max_data_length = MAX_LENGTH;
  buffer->data = malloc (INIT_LENGTH);
  buffer->position = 0;
  
  return buffer;
}

/**
 * Create an instance with an initial max, fixed size.
 */
ByteBuffer *byte_buffer_create_sized (size_t initial_size)
{
  ByteBuffer *buffer = malloc (sizeof (ByteBuffer));
    
  buffer->data_length = initial_size;
  buffer->max_data_length = initial_size;
  buffer->data = malloc (initial_size);
  buffer->position = 0;
  
  return buffer;
}

void byte_buffer_destroy (ByteBuffer *buffer)
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
  error_return (auto_resize_to_fit (buffer, position, error));
    
  buffer->position = position;

  return true;
}

bool auto_resize_to_fit (ByteBuffer *buffer, size_t min_length,
                         ElvinError *error)
{
  size_t new_length;
  
  if (buffer->data_length < min_length)
  {
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
  
  memcpy ((void *)new_data, (void *)old_data, buffer->position);
  
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

bool byte_buffer_read_int32 (ByteBuffer *buffer, uint32_t *value, 
                             ElvinError *error)
{
  uint32_t xdr_value;
  
  error_return (check_remaining (buffer, 4, error));
  
  xdr_value = *(uint32_t *)(buffer->data + buffer->position);
  
  *value = ntohl (xdr_value);
  
  buffer->position += 4;
  
  return true;
}

bool byte_buffer_write_int32 (ByteBuffer *buffer, uint32_t value, 
                              ElvinError *error)
{
  error_return (auto_resize_to_fit (buffer, buffer->position + 4, error));
  
  *(uint32_t *)(buffer->data + buffer->position) = htonl (value); 
  
  buffer->position += 4;
  
  return true;
}

bool byte_buffer_write_string (ByteBuffer *buffer, const char *string, 
                               ElvinError *error)
{
  uint32_t length = strlen (string);
  
  error_return 
    (auto_resize_to_fit (buffer, buffer->position + length + 4, error));
  
  byte_buffer_write_int32 (buffer, length, error);
  
  return byte_buffer_write_bytes (buffer, (uint8_t *)string, length, error);
}

const char *byte_buffer_read_string (ByteBuffer *buffer, ElvinError *error)
{
  uint32_t length;
  char *string;
  
  error_return (byte_buffer_read_int32 (buffer, &length, error));
  
  string = malloc (length + 1);
  
  if (string == NULL)
  {
    elvin_error_set (error, ERRNO_TO_ELVIN_ERROR (errno), 
                     "Not enough memory to allocate string");
    
    return NULL;
  }
  
  if (!byte_buffer_read_bytes (buffer, (uint8_t *)string, length, error))
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
  error_return (check_remaining (buffer, bytes_len, error));
  
  memcpy (bytes, buffer->data + buffer->position, bytes_len);
    
  buffer->position += bytes_len;
    
  return true;
}

bool byte_buffer_write_bytes (ByteBuffer *buffer, uint8_t *bytes, 
                              size_t bytes_len, ElvinError *error)
{
  error_return (auto_resize_to_fit (buffer, 
                                    buffer->position + bytes_len, error));
  
  memcpy (buffer->data + buffer->position, bytes, bytes_len);
  
  buffer->position += bytes_len;
  
  return true;
}