#ifndef ELVIN_NAMED_VALUES_H
#define ELVIN_NAMED_VALUES_H

#include <elvin/stdtypes.h>
#include <elvin/errors.h>
#include <elvin/values.h>

#ifdef AVIS_SHARED_LIB
  #include <hashtable_private.h>
#else
  /*
   * A dodgy way of allowing this to be used by clients without needing
   * to expose the full hashtable implementation headers.
   */
  struct entry
  {
    void *k, *v;
    unsigned int h;
    struct entry *next;
  };

  struct hashtable
  {
    unsigned int tablelength;
    struct entry **table;
    unsigned int entrycount;
    unsigned int loadlimit;
    unsigned int primeindex;
    unsigned int (*hashfn) (void *k);
    int (*eqfn) (void *k1, void *k2);
  };
#endif

/**
 * A map of string names to polymorphic Value instances. This is used as the 
 * payload for notification messages (see elvin_send()) and for specifying
 * connection options to the router (see elvin_open()).
 * 
 * @see named_values_create()
 * @see Value
 */
typedef struct hashtable NamedValues;

extern NamedValues _empty_named_values;

#define EMPTY_NAMED_VALUES (&_empty_named_values)

/**
 * Create a new named values instance on the heap.
 * 
 * @see named_values_free() 
 * @see named_values_destroy() 
 */
NamedValues *named_values_create ();

/**
 * Free and NULL a named values instance.
 * 
 * @see named_values_free()
 */
#define named_values_destroy(values) \
  (named_values_free (values), values = NULL)

/**
 * Free resources held by a named values instance.
 * 
 * @see named_values_create()
 */
void named_values_free (NamedValues *values);

/**
 * The number of entries in a set of named values.
 */
unsigned int named_values_size (NamedValues *values);

/**
 * Set the value mapped to a name. If an existing value exists, it will be
 * replaced and deleted.
 * 
 * @param values The values to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name.
 * 
 * @see named_values_get()
 * @see named_values_remove()
 */
void named_values_set (NamedValues *values, const char *name, Value *value);

/**
 * Get the value mapped to a name.
 * 
 * @param values The values to use.
 * @param name The name to lookup
 * @return The value associated with name, or NULL if no value.
 * 
 * @see named_values_set()
 */
Value *named_values_get (NamedValues *values, const char *name);

/**
 * Remove the value mapped to a name.
 * 
 * @param name The name to remove.
 * @return The value associated with name, or NULL if no value. This value
 * must be deallocated by the caller with value_destroy() when no longer 
 * needed.
 * 
 * @see named_values_set()
 */
Value *named_values_remove (NamedValues *values, const char *name);

/**
 * Convenience to set an int32 value.
 * 
 * @param values The values to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name.
 * 
 * @see named_values_set()
 */
#define named_values_set_int32(values, name, value) \
  (named_values_set (values, name, value_create_int32 (value)))

/**
 * Convenience to get an int32 value.
 * 
 * @param values The values to read from.
 * @param name The name to use.
 * @return The integer associated with name, or 0 if not set or value
 * is not an integer.
 * 
 * @see named_values_get()
 */
int32_t named_values_get_int32 (NamedValues *values, const char *name);

/**
 * Convenience to set an int64 value.
 * 
 * @param values The values to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name.
 * 
 * @see named_values_set()
 */
#define named_values_set_int64(values, name, value) \
  (named_values_set (values, name, value_create_int64 (value)))

/**
 * Convenience to get an int64 value.
 * 
 * @param values The values to read from.
 * @param name The name to use.
 * @return The integer associated with name, or 0 if not set or value
 * is not an integer.
 * 
 * @see named_values_get()
 */
int64_t named_values_get_int64 (NamedValues *values, const char *name);

/**
 * Convenience to set a real64 value.
 * 
 * @param values The values to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name.
 * 
 * @see named_values_set()
 */
#define named_values_set_real64(values, name, value) \
  (named_values_set (values, name, value_create_real64 (value)))

/**
 * Convenience to get a real64 value.
 * 
 * @param values The values to read from.
 * @param name The name to use.
 * @return The real64  associated with name, or 0 if not set or value
 * is not an real64 value.
 * 
 * @see named_values_get()
 */
real64_t named_values_get_real64 (NamedValues *values, const char *name);

/**
 * Convenience to set a string value.
 * 
 * @param values The values to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name. The string will be copied
 * before being added to the set.
 * 
 * @see named_values_set()
 */
#define named_values_set_string(values, name, value) \
  (named_values_set (values, name, value_create_string (value)))

/**
 * Convenience to get a string value.
 * 
 * @param values The values to read from.
 * @param name The name to use.
 * @return The string associated with name, or NULL if not set or value
 * is not a string.
 * 
 * @see named_values_get()
 */
const char *named_values_get_string (NamedValues *values, const char *name);

/**
 * Convenience to set an opaque value.
 * 
 * @param values The values to update.
 * @param name The name to use. This will be copied before being put into
 * the set.
 * @param value The value to associate with name. Unlike string values, 
 * this will NOT be copied before being added to the set.
 * 
 * @see named_values_set()
 */
#define named_values_set_opaque(values, name, value) \
  (named_values_set (values, name, value_create_opaque (value)))

Array *named_values_get_opaque (NamedValues *values, const char *name);

#endif /* ELVIN_NAMED_VALUES_H */
