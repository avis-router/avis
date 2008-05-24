#include <stdint.h>
#include <stdarg.h>
#include <stdlib.h>

#include <elvin/values.h>

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
    value->value.bytes = va_arg (args, uint8_t *);
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
    free (value->value.bytes);
}