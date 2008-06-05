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
 * @see named_values_init()
 * @see Value
 */
typedef struct hashtable NamedValues;

extern NamedValues _empty_named_values;

#define EMPTY_NAMED_VALUES (&_empty_named_values)

#define named_values_destroy(values) \
  (named_values_free (values), values = NULL)

NamedValues *named_values_create ();

void named_values_free (NamedValues *values);

unsigned int named_values_size (NamedValues *values);

void named_values_set (NamedValues *values, const char *name, Value *value);

Value *named_values_get (NamedValues *values, const char *name);

void named_values_set_int32 (NamedValues *values, const char *name, 
                             int32_t value);

int32_t named_values_get_int32 (NamedValues *values, const char *name);

void named_values_set_string (NamedValues *values, 
                              const char *name, const char *value);

const char *named_values_get_string (NamedValues *values, const char *name);

#endif /* ELVIN_NAMED_VALUES_H */
