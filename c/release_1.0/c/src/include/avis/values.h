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
/** \file
 * Polymorphic values.
 */
#ifndef AVIS_VALUES_H_
#define AVIS_VALUES_H_

#include <string.h>

#include <avis/stdtypes.h>
#include <avis/arrays.h>

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
  /** The type of value. */
  ValueType type;

  /** The actual wrapped value (discriminated by type). */
  union
  {
    int32_t   int32;
    int64_t   int64;
    real64_t  real64;
    char *    str;
    Array     bytes;
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
 * value_free(), so constant strings passed in here should be copied
 * before being used in a value that will be value_free()'d.
 * No type checking is done for the value parameter.
 */
Value *value_init (Value *value, ValueType type, ...);

/**
 * Free any resources held by a value instance.
 */
void value_free (Value *value);

/**
 * Destroy (free and NULL) a value instance.
 */
#define value_destroy(value) \
  (value_free (value), free (value), value = NULL)

/**
 * Allocate and init an int32 value. Use value_destroy() when done.
 */
#define value_create_int32(value) \
  (value_init ((Value *)avis_emalloc (sizeof (Value)), TYPE_INT32, (int32_t)(value)))

/**
 * Allocate and init an int64 value. Use value_destroy() when done.
 */
#define value_create_int64(value) \
  (value_init ((Value *)avis_emalloc (sizeof (Value)), TYPE_INT64, (int64_t)(value)))

/**
 * Allocate and init a real64 value. Use value_destroy() when done.
 */
#define value_create_real64(value) \
  (value_init ((Value *)avis_emalloc (sizeof (Value)), TYPE_REAL64, (real64_t)(value)))

/**
 * Allocate and init a string value. Use value_destroy() when done. The
 * string is duplicated before being added.
 *
 * @see value_init()
 * @see value_create_string_nocopy()
 */
#define value_create_string(value) \
  (value_init ((Value *)avis_emalloc (sizeof (Value)), TYPE_STRING, \
   avis_estrdup (value)))

/**
 * Allocate and init a string value, without copying it first. Use
 * value_destroy() when done, which will free the string passed in here.
 *
 * @see value_init()
 * @see value_create_string()
 */
#define value_create_string_nocopy(value) \
  (value_init ((Value *)avis_emalloc (sizeof (Value)), TYPE_STRING, value))

/**
 * Allocate and init an opaque value. Use value_destroy() when done.
 * Unlike string values, this will NOT be copied before being added to the
 * set.
 *
 * @param value An Array instance representing the opaque data.
 *
 * @see value_init()
 */
#define value_create_opaque(value) \
  (value_init ((Value *)avis_emalloc (sizeof (Value)), TYPE_OPAQUE, value))

/**
 * Initialise an array.
 *
 * @param array The array to initialise.
 * @param item_count The initial item count.
 * @param item_length The length of an item.
 *
 * @see array_create()
 */
Array *array_init (Array *array, size_t item_count, size_t item_length);

/**
 * Free resources held by an array.
 */
void array_free (Array *array);

/**
 * Create a new array instance on the heap.
 *
 * @see array_destroy()
 * @see array_init()
 */
#define array_create(item_type, item_count) \
  (array_init ((Array *)avis_emalloc (sizeof (Array)), \
               item_count, sizeof (item_type)))

/**
 * Destroy an array created with array_create().
 */
#define array_destroy(value) \
  (array_free (value), free (value), value = NULL)

/**
 * Get an item of a given type.
 *
 * @param array The array.
 * @param index The index of the item.
 * @param item_type The type of items in the array.
 */
#define array_get(array, index, item_type) \
  (((item_type *)array->items) [index])

/**
 * Test if two arrays are bitwise identical.
 */
bool array_equals (Array *array1, Array *array2);

#endif /*AVIS_VALUES_H_*/
