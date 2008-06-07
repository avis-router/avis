#ifndef ELVIN_NAMED_VALUES_H
#define ELVIN_NAMED_VALUES_H

#include <avis/stdtypes.h>
#include <avis/errors.h>
#include <avis/values.h>

struct hashtable;

/**
 * A map of string names to polymorphic Value instances. This is used as the 
 * payload for notification messages (see elvin_send()) and for specifying
 * connection options to the router (see elvin_open()).
 * 
 * @see attributes_create()
 * @see Value
 */
typedef struct
{
  struct hashtable *table;
} Attributes;

extern Attributes _empty_attributes;

#define EMPTY_NAMED_VALUES (&_empty_attributes)

/**
 * Create a new named attributes instance on the heap.
 * 
 * @see attributes_free() 
 * @see attributes_destroy() 
 */
#define attributes_create() \
  (attributes_init (malloc (sizeof (Attributes))))

Attributes *attributes_init (Attributes *);

/**
 * Free and NULL a named attributes instance.
 * 
 * @see attributes_free()
 */
#define attributes_destroy(attributes) \
  (attributes_free (attributes), attributes = NULL)

/**
 * Free resources held by a named attributes instance.
 * 
 * @see attributes_create()
 */
void attributes_free (Attributes *attributes);

/**
 * The number of entries in a set of named attributes.
 */
unsigned int attributes_size (Attributes *attributes);

/**
 * Set the value mapped to a name. If an existing value exists, it will be
 * replaced and deleted.
 * 
 * @param attributes The attributes to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name.
 * 
 * @see attributes_get()
 * @see attributes_remove()
 */
void attributes_set (Attributes *attributes, const char *name, Value *value);

/**
 * Get the value mapped to a name.
 * 
 * @param attributes The attributes to use.
 * @param name The name to lookup
 * @return The value associated with name, or NULL if no value.
 * 
 * @see attributes_set()
 */
Value *attributes_get (Attributes *attributes, const char *name);

/**
 * Remove the value mapped to a name.
 * 
 * @param attributes The attributes to modify.
 * @param name The name to remove.
 * 
 * @return The value associated with name, or NULL if no value. This value
 * must be deallocated by the caller with value_destroy() when no longer 
 * needed.
 * 
 * @see attributes_set()
 */
Value *attributes_remove (Attributes *attributes, const char *name);

/**
 * Convenience to set an int32 value.
 * 
 * @param attributes The attributes to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name.
 * 
 * @see attributes_set()
 */
#define attributes_set_int32(attributes, name, value) \
  (attributes_set (attributes, name, value_create_int32 (value)))

/**
 * Convenience to get an int32 value.
 * 
 * @param attributes The attributes to read from.
 * @param name The name to use.
 * @return The integer associated with name, or 0 if not set or value
 * is not an integer.
 * 
 * @see attributes_get()
 */
int32_t attributes_get_int32 (Attributes *attributes, const char *name);

/**
 * Convenience to set an int64 value.
 * 
 * @param attributes The attributes to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name.
 * 
 * @see attributes_set()
 */
#define attributes_set_int64(attributes, name, value) \
  (attributes_set (attributes, name, value_create_int64 (value)))

/**
 * Convenience to get an int64 value.
 * 
 * @param attributes The attributes to read from.
 * @param name The name to use.
 * @return The integer associated with name, or 0 if not set or value
 * is not an integer.
 * 
 * @see attributes_get()
 */
int64_t attributes_get_int64 (Attributes *attributes, const char *name);

/**
 * Convenience to set a real64 value.
 * 
 * @param attributes The attributes to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name.
 * 
 * @see attributes_set()
 */
#define attributes_set_real64(attributes, name, value) \
  (attributes_set (attributes, name, value_create_real64 (value)))

/**
 * Convenience to get a real64 value.
 * 
 * @param attributes The attributes to read from.
 * @param name The name to use.
 * @return The real64  associated with name, or 0 if not set or value
 * is not an real64 value.
 * 
 * @see attributes_get()
 */
real64_t attributes_get_real64 (Attributes *attributes, const char *name);

/**
 * Convenience to set a string value.
 * 
 * @param attributes The attributes to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name. The string will be copied
 * before being added to the set.
 * 
 * @see attributes_set()
 */
#define attributes_set_string(attributes, name, value) \
  (attributes_set (attributes, name, value_create_string (value)))

/**
 * Convenience to get a string value.
 * 
 * @param attributes The attributes to read from.
 * @param name The name to use.
 * @return The string associated with name, or NULL if not set or value
 * is not a string.
 * 
 * @see attributes_get()
 */
const char *attributes_get_string (Attributes *attributes, const char *name);

/**
 * Convenience to set an opaque value.
 * 
 * @param attributes The attributes to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name. Unlike string attributes, 
 * this will NOT be copied before being added to the set.
 * 
 * @see attributes_set()
 */
#define attributes_set_opaque(attributes, name, value) \
  (attributes_set (attributes, name, value_create_opaque (value)))

Array *attributes_get_opaque (Attributes *attributes, const char *name);

#endif /* ELVIN_NAMED_VALUES_H */
