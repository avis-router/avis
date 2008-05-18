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
} ByteBuffer;

ByteBuffer *byte_buffer_create ();

ByteBuffer *byte_buffer_create_sized (size_t initial_size);

void byte_buffer_destroy (ByteBuffer *buffer);

bool byte_buffer_read_int32 (ByteBuffer *buffer, uint32_t *value, 
                             ElvinError *error);

bool byte_buffer_write_int32 (ByteBuffer *buffer, uint32_t value, 
                              ElvinError *error);

bool byte_buffer_read_bytes (ByteBuffer *buffer, uint8_t *bytes, 
                             size_t bytes_len, ElvinError *error);

bool byte_buffer_write_bytes (ByteBuffer *buffer, uint8_t *bytes, 
                              size_t bytes_len, ElvinError *error);

bool byte_buffer_set_position (ByteBuffer *buffer, size_t position,
                               ElvinError *error);

void byte_buffer_set_max_length (ByteBuffer *buffer, int max_capacity);

void byte_buffer_ensure_capacity (ByteBuffer *buffer, size_t capacity);

#define byte_buffer_len(buffer) (buffer->data_length)
#define byte_buffer_position(buffer) (buffer->position)
#define byte_buffer_set_max_length(buffer, max_capacity) \
  (buffer->max_data_length = (max_capacity))
#define byte_buffer_skip(buffer, skip_bytes, error) \
  (byte_buffer_set_position ((buffer), buffer->position + (skip_bytes), (error)))

#endif /*BYTE_BUFFER_H_*/
