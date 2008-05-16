#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include <netinet/in.h>

#include <elvin/errors.h>

#include "byte_buffer.h"

#define INIT_LENGTH 1024
#define MAX_LENGTH (2 * 1024 * 1024)

static bool check_remaining (Byte_Buffer *buffer, size_t required_remaining, 
                             Elvin_Error *error);

static bool auto_resize_to_fit (Byte_Buffer *buffer, size_t min_length,
                                Elvin_Error *error);

static void expand_buffer (Byte_Buffer *buffer, size_t new_length);

Byte_Buffer *byte_buffer_create ()
{
  Byte_Buffer *buffer = malloc (sizeof (Byte_Buffer));
  
  buffer->data_length = INIT_LENGTH;
  buffer->max_data_length = MAX_LENGTH;
  buffer->data = malloc (INIT_LENGTH);
  buffer->position = 0;
  
  return buffer;
}

void byte_buffer_destroy (Byte_Buffer *buffer)
{
  if (buffer->data)
  {
    free (buffer->data);
    buffer->data = NULL;
    buffer->data_length = -1;
  }
}

bool byte_buffer_set_position (Byte_Buffer *buffer, size_t position,
                               Elvin_Error *error)
{
  error_return (auto_resize_to_fit (buffer, position, error));
    
  buffer->position = position;

  return true;
}

bool auto_resize_to_fit (Byte_Buffer *buffer, size_t min_length,
                         Elvin_Error *error)
{
  if (buffer->data_length < min_length)
  {
    if (min_length > buffer->max_data_length)
      return elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Buffer overflow");

    // new length = min_length rounded up to nearest ^ 2
    // TODO could do this more efficiently
    size_t new_length = buffer->data_length;
    
    while (new_length < min_length)
      new_length *= 2;
    
    expand_buffer (buffer, new_length);
  }
  
  return true;
}

void expand_buffer (Byte_Buffer *buffer, size_t new_length)
{
  uint8_t *new_data = malloc (new_length);
  uint8_t *old_data = buffer->data;
  
  memcpy ((void *)new_data, (void *)old_data, buffer->position);
  
  buffer->data = new_data;
  buffer->data_length = new_length;
  
  free (old_data);
}

void byte_buffer_ensure_capacity (Byte_Buffer *buffer, size_t capacity)
{
  if (buffer->data_length < capacity)
    expand_buffer (buffer, capacity);
}

bool check_remaining (Byte_Buffer *buffer, size_t required_remaining, 
                      Elvin_Error *error)
{
  if (buffer->data_length - buffer->position < required_remaining)
    return elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Buffer underflow");
  else
    return true;
}

bool byte_buffer_read_int32 (Byte_Buffer *buffer, uint32_t *value, 
                             Elvin_Error *error)
{
  error_return (check_remaining (buffer, 4, error));
  
  uint32_t v = *(uint32_t *)(buffer->data + buffer->position);
  
  *value = ntohl (v);
  
  buffer->position += 4;
  
  return true;
}

bool byte_buffer_write_int32 (Byte_Buffer *buffer, uint32_t value, 
                              Elvin_Error *error)
{
  error_return (auto_resize_to_fit (buffer, buffer->position + 4, error));
  
  *(uint32_t *)(buffer->data + buffer->position) = htonl (value); 
  
  buffer->position += 4;
  
  return true;
}

bool byte_buffer_read_bytes (Byte_Buffer *buffer, uint8_t *bytes, 
                             size_t bytes_len, Elvin_Error *error)
{
  error_return (check_remaining (buffer, bytes_len, error));
  
  memcpy ((void *)bytes, (void *)(buffer->data + buffer->position), bytes_len);
    
  buffer->position += bytes_len;
    
  return true;
}

bool byte_buffer_write_bytes (Byte_Buffer *buffer, uint8_t *bytes, 
                              size_t bytes_len, Elvin_Error *error)
{
  error_return (auto_resize_to_fit (buffer, buffer->position + bytes_len, error));
  
  memcpy ((void *)(buffer->data + buffer->position), (void *)bytes, bytes_len);
  
  buffer->position += bytes_len;
  
  return true;
}