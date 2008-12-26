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
 * Elvin attributes (name/value pairs) support.
 */
#ifndef AVIS_ATTRIBUTES_H
#define AVIS_ATTRIBUTES_H

#include <avis/stdtypes.h>
#include <avis/errors.h>
#include <avis/values.h>

struct hashtable;

/**
 * A map of string names to polymorphic Value instances. This is used as the
 * payload for notification messages (see elvin_send()) and for specifying
 * connection options to the router (see elvin_open()).
 *
 * All string keys and Value's associated with them are considered to
 * be owned by the Attributes instance and will be automatically freed
 * when appropriate. For convenience, most of the methods used to add
 * entries automatically copy their string name parameter before
 * adding: see the documentation for each function for more
 * information.
 *
 * @see attributes_create()
 * @see AttributesIter
 * @see Value
 */
typedef struct
{
  struct hashtable *table;
} Attributes;

/**
 * An iterator over an Attributes collection.
 *
 * @see attributes_iter_init()
 */
typedef struct
{
  /* below is a copy of hashtable_iter from hashtable_iter.h */
  struct
  {
    void *h;
    void *e;
    void *parent;
    unsigned int index;
  } hash_iter;
  
  bool has_next;
} AttributesIter;

AVIS_PUBLIC_DATA 
Attributes _empty_attributes;

#define EMPTY_ATTRIBUTES (&_empty_attributes)

/**
 * Create a new attributes instance on the heap.
 *
 * @see attributes_init()
 * @see attributes_free()
 * @see attributes_destroy()
 */
#define attributes_create() \
  (attributes_init ((Attributes *)avis_emalloc (sizeof (Attributes))))

/**
 * Initialise an attributes instance to empty.
 *
 * @see attributes_create()
 * @see attributes_clear()
 */
AVIS_PUBLIC
Attributes *attributes_init (Attributes *);

/**
 * Free and NULL a named attributes instance.
 *
 * @see attributes_free()
 */
#define attributes_destroy(attributes) \
  if ((attributes) && (attributes) != EMPTY_ATTRIBUTES) \
  { \
    attributes_free (attributes), free (attributes), attributes = NULL; \
  }

/**
 * Free resources held by a named attributes instance.
 *
 * @see attributes_create()
 * @see attributes_clear()
 */
AVIS_PUBLIC
void attributes_free (Attributes *attributes);

/**
 * Clear and deallocate all entries, leaving an empty set of attributes.
 */
AVIS_PUBLIC
void attributes_clear (Attributes *attributes);

/**
 * Copy a set of attributes from a source to a target. Copies the values also.
 *
 * @param target The target for the copied attributes.
 * @param source The source to copy from.
 * @return A pointer to the target.
 *
 * @see attributes_clone()
 */
AVIS_PUBLIC
Attributes *attributes_copy (Attributes *target, const Attributes *source);

/**
 * Create a new set of attributes cloned from a source set.
 *
 * @param source The source to copy from.
 * @return A pointer to the target.
 *
 * @see attributes_copy()
 */
#define attributes_clone(source) attributes_copy (attributes_create (), source)

/**
 * The number of entries in a set of named attributes.
 */
AVIS_PUBLIC
unsigned int attributes_size (Attributes *attributes);

/**
 * Create and initialise an AttributesIter instance.
 */
#define attributes_iter_create(attributes) \
  attributes_iter_init \
    ((AttributesIter *)avis_emalloc (sizeof (AttributesIter)), (attributes))

/** 
 * Free and NULL an attributes iterator.
 */
#define attributes_iter_destroy(iter) (free (iter), (iter) = NULL)

/**
 * Initialise an attributes iterator to iterate over the given
 * attributes set.
 * <p>
 * Example usage:
 * <pre>
 * Attributes *attrs = ...;
 * AttributesIter i;
 * 
 * attributes_iter_init (&i, attrs);
 * 
 * while (attributes_iter_has_next (&i))
 * {
 *   const char *name = attributes_iter_name (&i);
 *   const Value *value = attributes_iter_value (&i);
 * 
 *   ...
 *
 *   attributes_iter_next (&i);
 * }
 * </pre>
 *
 * @see attributes_iter_has_next()
 * @see attributes_iter_next()
 * @see attributes_iter_name()
 * @see attributes_iter_value()
 */
AVIS_PUBLIC
AttributesIter *attributes_iter_init (AttributesIter *iter, 
                                      const Attributes *attributes);

/**
 * Returns the name of the attribute pointed to by the iterator, or
 * NULL if the iterator is finished.
 */
AVIS_PUBLIC
const char *attributes_iter_name (const AttributesIter *iter);

/**
 * Returns the value of the attribute pointed to by the iterator, or
 * NULL if the iterator is finished.
 */
AVIS_PUBLIC
const Value *attributes_iter_value (const AttributesIter *iter);

#define attributes_iter_has_next(iter) ((iter)->has_next)

/**
 * Advanced the iterator to the next attribute.
 *
 * @return True if attributes_iter_has_next() would return true after
 * the advance (i.e. the iterator points to an attribute).
 */
AVIS_PUBLIC
bool attributes_iter_next (AttributesIter *iter);

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
 * @see attributes_set_direct()
 * @see attributes_remove()
 */
AVIS_PUBLIC
void attributes_set (Attributes *attributes, const char *name, Value *value);

/**
 * Same as attributes_set(), but does not copy the name first. If an
 * existing value exists, it will be replaced and deleted.
 *
 * @param attributes The attributes to update.
 * @param name The name to use.
 * @param value The value to associate with name.
 *
 * @see attributes_get()
 * @see attributes_set()
 * @see attributes_remove()
 * @see value_create_string_nocopy()
 */
AVIS_PUBLIC
void attributes_set_nocopy (Attributes *attributes, char *name, Value *value);

/**
 * Get the value mapped to a name.
 *
 * @param attributes The attributes to use.
 * @param name The name to lookup
 * @return The value associated with name, or NULL if no value.
 *
 * @see attributes_set()
 */
AVIS_PUBLIC
Value *attributes_get (Attributes *attributes, const char *name);

/**
 * Test if the attributes contains a mapping for a given field name.
 */
AVIS_PUBLIC
bool attributes_contains (Attributes *attributes, const char *name);

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
AVIS_PUBLIC
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
 * @see attributes_set_int32()
 * @see attributes_get()
 */
AVIS_PUBLIC
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
 * @see attributes_set_int64()
 * @see attributes_get()
 */
AVIS_PUBLIC
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
 * @see attributes_set_real64()
 * @see attributes_get()
 */
AVIS_PUBLIC
real64_t attributes_get_real64 (Attributes *attributes, const char *name);

/**
 * Convenience to set a string value.
 *
 * @param attributes The attributes to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name. The string will be
 * copied before being added to the set. This must be a valid UTF-8
 * string: an invalid UTF-8 string will likely trigger a protocol
 * violation in the router and result in disconnection.
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
 * @see attributes_set_string()
 * @see attributes_get()
 */
AVIS_PUBLIC
const char *attributes_get_string (Attributes *attributes, const char *name);

/**
 * Convenience to set an opaque value.
 *
 * @param attributes The attributes to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The opaque value to associate with name (an Array
 * instance). Unlike string attributes, this will NOT be copied before
 * being added to the set: the set will take ownership of the array's
 * data and free it when appropriate.
 *
 * @see attributes_set()
 */
#define attributes_set_opaque(attributes, name, value) \
  (attributes_set (attributes, name, value_create_opaque (value)))

/**
 * Convenience to get an opaque value.
 *
 * @param attributes The attributes to read from.
 * @param name The name to use.
 * @return The opaque value associated with name, or NULL if not set
 * or value is not an opaque.
 *
 * @see attributes_set_opaque()
 * @see attributes_get()
 */
AVIS_PUBLIC
Array *attributes_get_opaque (Attributes *attributes, const char *name);

#endif /* AVIS_ATTRIBUTES_H */
