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
#ifndef BYTE_BUFFER_H_
#define BYTE_BUFFER_H_

#include <stdlib.h>

#include <avis/stdtypes.h>
#include <avis/values.h>
#include <avis/errors.h>

#include "errors_private.h"

typedef struct
{
  uint8_t * data;
  size_t    data_length;
  size_t    max_data_length;
  size_t    position;
} ByteBuffer;

#define byte_buffer_create() \
  (byte_buffer_init ((ByteBuffer *)emalloc (sizeof (ByteBuffer))))

AVIS_PUBLIC
ByteBuffer *byte_buffer_init (ByteBuffer *buffer);

AVIS_PUBLIC
ByteBuffer *byte_buffer_init_sized (ByteBuffer *buffer, size_t initial_size);

#define byte_buffer_destroy(buffer) \
  (byte_buffer_free (buffer), free (buffer), buffer = NULL)

AVIS_PUBLIC
void byte_buffer_free (ByteBuffer *buffer);

AVIS_PUBLIC
int32_t byte_buffer_read_int32 (ByteBuffer *buffer, ElvinError *error);

AVIS_PUBLIC
bool byte_buffer_write_int32 (ByteBuffer *buffer, int32_t value,
                              ElvinError *error);

AVIS_PUBLIC
int64_t byte_buffer_read_int64 (ByteBuffer *buffer, ElvinError *error);

AVIS_PUBLIC
bool byte_buffer_write_int64 (ByteBuffer *buffer, int64_t value,
                              ElvinError *error);

AVIS_PUBLIC
real64_t byte_buffer_read_real64 (ByteBuffer *buffer, ElvinError *error);

AVIS_PUBLIC
bool byte_buffer_write_real64 (ByteBuffer *buffer, real64_t value,
                               ElvinError *error);

AVIS_PUBLIC
bool byte_buffer_write_string (ByteBuffer *buffer, const char *string,
                               ElvinError *error);

AVIS_PUBLIC
char *byte_buffer_read_string (ByteBuffer *buffer, ElvinError *error);

AVIS_PUBLIC
bool byte_buffer_read_bytes (ByteBuffer *buffer, uint8_t *bytes,
                             size_t bytes_len, ElvinError *error);

AVIS_PUBLIC
bool byte_buffer_write_bytes (ByteBuffer *buffer, uint8_t *bytes,
                              size_t bytes_len, ElvinError *error);

AVIS_PUBLIC
bool byte_buffer_read_byte_array (ByteBuffer *buffer, Array *array,
                                  ElvinError *error);

AVIS_PUBLIC
bool byte_buffer_write_byte_array (ByteBuffer *buffer, Array *array,
                                   ElvinError *error);

AVIS_PUBLIC
bool byte_buffer_set_position (ByteBuffer *buffer, size_t position,
                               ElvinError *error);

AVIS_PUBLIC
void byte_buffer_set_max_length (ByteBuffer *buffer, int max_capacity);

AVIS_PUBLIC
bool byte_buffer_ensure_capacity (ByteBuffer *buffer, size_t min_length,
                                  ElvinError *error);

#define byte_buffer_set_max_length(buffer, max_capacity) \
  (buffer->max_data_length = (max_capacity))

#define byte_buffer_skip(buffer, skip_bytes, error) \
  (byte_buffer_set_position ((buffer), (buffer)->position + (skip_bytes), (error)))

#endif /*BYTE_BUFFER_H_*/
