/*
 *  named_values.h
 *  Elvin Client
 *
 *  Created by Matthew Phillips on 13/05/08.
 *  Copyright 2008 __MyCompanyName__. All rights reserved.
 *
 */

#ifndef ELVIN_NAMED_VALUES_H
#define ELVIN_NAMED_VALUES_H

#include <elvin/stdtypes.h>
#include <elvin/errors.h>

struct hashtable;

/* TODO */
#define EMPTY_NAMED_VALUES NULL

typedef struct 
{
  struct hashtable *table;
} NamedValues;

#define named_values_create() \
  (named_values_init (malloc (sizeof (NamedValues))))

#define named_values_destroy(values) \
  (named_values_free (values), free (values))

NamedValues *named_values_init (NamedValues *values);

void named_values_free (NamedValues *values);

void named_values_set_int32 (NamedValues *values, const char *name, 
                             uint32_t value);

uint32_t named_values_get_int32 (NamedValues *values, const char *name);

#define named_values_size(values) (hashtable_count ((values)->table))

#endif /* ELVIN_NAMED_VALUES_H */
