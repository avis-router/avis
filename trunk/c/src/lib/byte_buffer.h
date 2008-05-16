#ifndef BYTE_BUFFER_H_
#define BYTE_BUFFER_H_

#include <stdint.h>
#include <stdlib.h>

typedef struct
{
  size_t data_length;
  size_t max_data_length;
  size_t position;
  uint8_t *data;
} Byte_Buffer;

Byte_Buffer *byte_buffer_create ();

void byte_buffer_destroy (Byte_Buffer *buffer);

bool byte_buffer_read_int32 (Byte_Buffer *buffer, uint32_t *value, 
                             Elvin_Error *error);

bool byte_buffer_write_int32 (Byte_Buffer *buffer, uint32_t value, 
                              Elvin_Error *error);

bool byte_buffer_read_bytes (Byte_Buffer *buffer, uint8_t *bytes, 
                             size_t bytes_len, Elvin_Error *error);

bool byte_buffer_write_bytes (Byte_Buffer *buffer, uint8_t *bytes, 
                              size_t bytes_len, Elvin_Error *error);

bool byte_buffer_set_position (Byte_Buffer *buffer, size_t position,
                               Elvin_Error *error);

void byte_buffer_set_max_length (Byte_Buffer *buffer, int max_capacity);

void byte_buffer_ensure_capacity (Byte_Buffer *buffer, size_t capacity);

#define byte_buffer_len(buffer) (buffer->data_length)
#define byte_buffer_position(buffer) (buffer->position)
#define byte_buffer_set_max_length(buffer, max_capacity) \
  (buffer->max_data_length = (max_capacity))
#define byte_buffer_skip(buffer, skip_bytes, error) \
  (byte_buffer_set_position ((buffer), buffer->position + (skip_bytes), (error)))

#endif /*BYTE_BUFFER_H_*/
