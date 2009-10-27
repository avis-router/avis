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
#include <stdarg.h>
#include <stdlib.h>

#include <avis/stdtypes.h>
#include <avis/errors.h>

#include "values_private.h"
#include "arrays_private.h"

Value *value_init (Value *value, ValueType type, ...)
{
  va_list args;

  value->type = type;
    
  va_start (args, type);
  
  switch (type)
  {
  case TYPE_INT32:
    value->value.int32 = va_arg (args, int32_t);
    break;
  case TYPE_INT64:
    value->value.int64 = va_arg (args, int64_t);
    break;
  case TYPE_REAL64:
    value->value.real64 = va_arg (args, real64_t);
    break;
  case TYPE_STRING:
    value->value.str = va_arg (args, char *);
    break;
  case TYPE_OPAQUE:
    value->value.bytes = va_arg (args, Array);
    break;
  }
  
  va_end (args);
  
  return value;
}

void value_free (Value *value)
{
  if (value->type == TYPE_STRING)
    free (value->value.str);
  else if (value->type == TYPE_OPAQUE)
    array_free (&value->value.bytes);
}

Value *value_copy (Value *target, const Value *source)
{
  target->type = source->type;
  
  switch (source->type)
  {
    case TYPE_INT32:
      target->value.int32 = source->value.int32;
      break;
    case TYPE_INT64:
      target->value.int64 = source->value.int64;
      break;
    case TYPE_REAL64:
      target->value.real64 = source->value.real64;
      break;
    case TYPE_STRING:
      target->value.str = estrdup (source->value.str);
      break;
    case TYPE_OPAQUE:
      array_copy (&target->value.bytes, &source->value.bytes, 1);
      break;
  }
  
  return target;
}

bool value_read (ByteBuffer *buffer, Value *value, ElvinError *error)
{
  uint32_t type = byte_buffer_read_int32 (buffer, error);
  
  if (elvin_error_occurred (error))
    return false;
  
  value->type = type;
  
  switch (type)
  {
  case TYPE_INT32:
    value->value.int32 = byte_buffer_read_int32 (buffer, error);
    break;
  case TYPE_INT64:
    value->value.int64 = byte_buffer_read_int64 (buffer, error);
    break;
  case TYPE_REAL64:
    value->value.real64 = byte_buffer_read_real64 (buffer, error);
    break;
  case TYPE_STRING:
    value->value.str = byte_buffer_read_string (buffer, error);
    break;
  case TYPE_OPAQUE:
    byte_buffer_read_byte_array (buffer, &value->value.bytes, error);
    break;
  default:
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, 
                     "Invalid value type: %u", type);
  }
  
  return elvin_error_ok (error);
}

bool value_write (ByteBuffer *buffer, Value *value, ElvinError *error)
{
  on_error_return_false
    (byte_buffer_write_int32 (buffer, value->type, error));
  
  switch (value->type)
  {
  case TYPE_INT32:
    byte_buffer_write_int32 (buffer, value->value.int32, error);
    break;
  case TYPE_INT64:
    byte_buffer_write_int64 (buffer, value->value.int64, error);
    break;
  case TYPE_REAL64:
    byte_buffer_write_real64 (buffer, value->value.real64, error);
    break;
  case TYPE_STRING:
    byte_buffer_write_string (buffer, value->value.str, error);
    break;
  case TYPE_OPAQUE:
    byte_buffer_write_byte_array (buffer, &value->value.bytes, error);
    break;
  default:
    abort ();
  }
  
  return true;
}
