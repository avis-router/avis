#ifndef BYTE_BUFFER_H_
#define BYTE_BUFFER_H_

#include <stdint.h>
#include <stdlib.h>

typedef struct
{
  size_t data_length;
  uint8_t *data;
} Byte_Buffer;

Byte_Buffer *byte_buffer_create ();
void byte_buffer_destroy (Byte_Buffer *buffer);
size_t byte_buffer_len (Byte_Buffer *buffer);

#endif /*BYTE_BUFFER_H_*/
