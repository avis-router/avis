#ifndef VALUES_H_
#define VALUES_H_

#include <string.h>

#include <elvin/stdtypes.h>

/**
 * The type tag for polymorphic values. These are the same as the type codes 
 * from the client protocol spec.
 */
typedef enum 
{
  TYPE_INT32 = 1, TYPE_INT64 = 2, TYPE_REAL64 = 3, 
  TYPE_STRING = 4, TYPE_OPAQUE = 5
} ValueType;

/** 
 * A polymorphic value: either an int32, int64, real64, string or opaque 
 * (an array of bytes). 
 * */
typedef struct
{
  ValueType type;
  
  union
  {
    int32_t   int32;
    int64_t   int64;
    real64_t  real64;
    char *    str;
    uint8_t * bytes; /* TODO this also needs a length parameter */
  } value;
} Value;

/** 
 * Initialise a polymorphic value.
 *  
 * @param value The value to init.
 * @param type  The type of value.
 * 
 * The next param is the value to be assigned. For strings and opaques, this 
 * value is "owned" by the poly value and will be free'd on calling
 * value_free(). No type checking is done for this value.  
 */
Value *value_init (Value *value, ValueType type, ...);

/**
 * Free any resources held by a value instance.
 */
void value_free (Value *value);

/**
 * Destroy (free and NULL) a value instance.
 */
#define value_destroy(value) (value_free (value), free (value), value = NULL)

/** Allocate and init an int32 value. Use value_destroy() when done. */
#define value_create_int32(value) \
  (value_init (malloc (sizeof (Value)), TYPE_INT32, value))

/** Allocate and init a string value. Use value_destroy() when done. */
#define value_create_string(value) \
  (value_init (malloc (sizeof (Value)), TYPE_STRING, strdup (value)))

#endif /*VALUES_H_*/
