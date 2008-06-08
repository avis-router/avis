#include <stdarg.h>
#include <stdlib.h>

#include <avis/stdtypes.h>
#include <avis/values.h>
#include <avis/errors.h>
#include <avis/log.h>

#include "byte_buffer.h"

Value *value_init (Value *value, ValueType type, ...)
{
  va_list args;

  value->type = type;
    
  va_start (args, type);
  
  switch (type)
  {
  case TYPE_INT32:
    value->value.int32 = va_arg (args, uint32_t);
    break;
  case TYPE_INT64:
    value->value.int64 = va_arg (args, uint64_t);
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
    byte_buffer_skip (buffer, 8, error);
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
    DIAGNOSTIC1 ("Invalid value type found %u", type);
    
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Invalid value type");
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

Array *array_init (Array *array, size_t item_count, size_t item_length)
{
  array->item_count = item_count;
  array->items = malloc (item_count * item_length);

  return array;
}

void array_free (Array *array)
{
  if (array->items)
  {
    free (array->items);
    
    array->items = NULL;
    array->item_count = 0;
  }
}

bool array_equals (Array *array1, Array *array2)
{
  return array1->item_count == array2->item_count && 
         memcmp (array1->items, array2->items, array1->item_count) == 0;
}