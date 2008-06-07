#ifndef BYTE_BUFFER_H_
#define BYTE_BUFFER_H_

#include <avis/stdtypes.h>
#include <avis/values.h>
#include <stdlib.h>

typedef struct
{
  uint8_t * data;
  size_t    data_length;
  size_t    max_data_length;
  size_t    position;
} ByteBuffer;

#define byte_buffer_create() (byte_buffer_init (malloc (sizeof (ByteBuffer))))

ByteBuffer *byte_buffer_init (ByteBuffer *buffer);

ByteBuffer *byte_buffer_init_sized (ByteBuffer *buffer, size_t initial_size);

#define byte_buffer_destroy(buffer) \
  (byte_buffer_free (buffer), free (buffer), buffer = NULL)

void byte_buffer_free (ByteBuffer *buffer);

int32_t byte_buffer_read_int32 (ByteBuffer *buffer, ElvinError *error);

bool byte_buffer_write_int32 (ByteBuffer *buffer, int32_t value, 
                              ElvinError *error);

int64_t byte_buffer_read_int64 (ByteBuffer *buffer, ElvinError *error);

bool byte_buffer_write_int64 (ByteBuffer *buffer, int64_t value, 
                              ElvinError *error);

real64_t byte_buffer_read_real64 (ByteBuffer *buffer, ElvinError *error);

bool byte_buffer_write_real64 (ByteBuffer *buffer, real64_t value, 
                               ElvinError *error);

bool byte_buffer_write_string (ByteBuffer *buffer, const char *string, 
                               ElvinError *error);

char *byte_buffer_read_string (ByteBuffer *buffer, ElvinError *error);

bool byte_buffer_read_bytes (ByteBuffer *buffer, uint8_t *bytes, 
                             size_t bytes_len, ElvinError *error);

bool byte_buffer_write_bytes (ByteBuffer *buffer, uint8_t *bytes, 
                              size_t bytes_len, ElvinError *error);

bool byte_buffer_read_byte_array (ByteBuffer *buffer, Array *array,
                                  ElvinError *error);

bool byte_buffer_write_byte_array (ByteBuffer *buffer, Array *array,
                                   ElvinError *error);

bool byte_buffer_set_position (ByteBuffer *buffer, size_t position,
                               ElvinError *error);

void byte_buffer_set_max_length (ByteBuffer *buffer, int max_capacity);

void byte_buffer_ensure_capacity (ByteBuffer *buffer, size_t capacity);

#define byte_buffer_set_max_length(buffer, max_capacity) \
  (buffer->max_data_length = (max_capacity))

#define byte_buffer_skip(buffer, skip_bytes, error) \
  (byte_buffer_set_position ((buffer), buffer->position + (skip_bytes), (error)))

#endif /*BYTE_BUFFER_H_*/
