#ifndef ELVIN_NAMED_VALUES_H
#define ELVIN_NAMED_VALUES_H

#include <elvin/stdtypes.h>
#include <elvin/errors.h>
#include <elvin/values.h>

struct hashtable;

typedef struct 
{
  struct hashtable *table;
} NamedValues;

extern NamedValues _empty_named_values;

#define EMPTY_NAMED_VALUES (&_empty_named_values)

#define named_values_create() \
  (named_values_init (malloc (sizeof (NamedValues))))

#define named_values_destroy(values) \
  (named_values_free (values), free (values), values = NULL)

NamedValues *named_values_init (NamedValues *values);

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
