#include "byte_buffer.h"

#define INIT_LENGTH 1024

Byte_Buffer *byte_buffer_create ()
{
  Byte_Buffer *buffer = malloc (sizeof (Byte_Buffer));
  
  buffer->data_length = INIT_LENGTH;
  buffer->data = malloc (INIT_LENGTH);
  
  return buffer;
}

void byte_buffer_destroy (Byte_Buffer *buffer)
{
  free (buffer->data);
}

size_t byte_buffer_len (Byte_Buffer *buffer)
{
  return buffer->data_length;
}