#include <stdio.h>
#include <string.h>

#include <hashtable.h>

#include <elvin/values.h>
#include <elvin/named_values.h>

#include "named_values_private.h"

static unsigned int string_hash (void *string);

static int string_equals (void *string1, void *string2);

NamedValues *named_values_init (NamedValues *values)
{
  values->table = create_hashtable (16, string_hash, string_equals);
  
  return values;
}

void named_values_free (NamedValues *values)
{
  if (values->table)
  {
    /* TODO: dealloc strings and opaques */
    hashtable_destroy (values->table, 1);
    
    values->table = NULL;
  }
}

void named_values_set_int32 (NamedValues *values, const char *name, 
                             uint32_t value)
{
  hashtable_insert (values->table, strdup (name), value_create_int32 (value));
}

uint32_t named_values_get_int32 (NamedValues *values, const char *name)
{
  Value *value = hashtable_search (values->table, (void *)name);
  
  if (value && value->type == TYPE_INT32)
    return value->value.int32;
  else
    return 0;
}

bool named_values_write (ByteBuffer *buffer, NamedValues *values, 
                         ElvinError *error)
{
  /* TODO */
  return false;
}

bool named_values_read (ByteBuffer *buffer, NamedValues *values, 
                        ElvinError *error)
{
  /* TODO */
  return false;
}

/*
 * djb2 algorithm from http://www.cse.yorku.ca/~oz/hash.html.
 */
unsigned int string_hash (void *string)
{
  unsigned int hash = 5381;
  int c;

  while ((c = *(const char *)string))
  {
    hash = ((hash << 5) + hash) + c; /* hash * 33 + c */
    
    string = ((char *)string) + 1;
  }
  
  return hash;
}

int string_equals (void *string1, void *string2)
{
  return strcmp (string1, string2) == 0;
}
