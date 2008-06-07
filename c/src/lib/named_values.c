#include <stdio.h>
#include <string.h>

#include "hashtable.h"
#include "hashtable_itr.h"

#include <elvin/values.h>
#include <elvin/named_values.h>

#include "values_private.h"
#include "named_values_private.h"

static struct hashtable empty_hashtable = {0, NULL, 0, 0, 0, NULL, NULL};

NamedValues _empty_named_values = {&empty_hashtable};

static unsigned int string_hash (void *string);

static int string_equals (void *string1, void *string2);

NamedValues *named_values_init (NamedValues *values)
{
  values->table = malloc (sizeof (struct hashtable));
  
  init_hashtable (values->table, 16, string_hash, string_equals);
  
  return values;
}

void named_values_free (NamedValues *values)
{
  /* free entries */
  if (hashtable_count (values->table) > 0)
  {
    struct hashtable_itr *i = hashtable_iterator (values->table);

    do
    {      
      free (hashtable_iterator_key (i));
      
      value_destroy (hashtable_iterator_value (i));
    } while (hashtable_iterator_advance (i));
    
    free (i);
  }

  hashtable_free (values->table, 0);
}

unsigned int named_values_size (NamedValues *values)
{
  return hashtable_count (values->table);
}

void named_values_set (NamedValues *values, const char *name, Value *value)
{
  Value *old_value = named_values_remove (values, name);
  
  if (old_value)
    value_destroy (old_value);
  
  hashtable_insert (values->table, strdup (name), value);
}

Value *named_values_get (NamedValues *values, const char *name)
{
  return hashtable_search (values->table, (void *)name);
}

Value *named_values_remove (NamedValues *values, const char *name)
{
  return hashtable_remove (values->table, (void *)name);
}

int32_t named_values_get_int32 (NamedValues *values, const char *name)
{
  Value *value = hashtable_search (values->table, (void *)name);
  
  if (value && value->type == TYPE_INT32)
    return value->value.int32;
  else
    return 0;
}

int64_t named_values_get_int64 (NamedValues *values, const char *name)
{
  Value *value = hashtable_search (values->table, (void *)name);
  
  if (value && value->type == TYPE_INT64)
    return value->value.int64;
  else
    return 0;
}

real64_t named_values_get_real64 (NamedValues *values, const char *name)
{
  Value *value = hashtable_search (values->table, (void *)name);
  
  if (value && value->type == TYPE_REAL64)
    return value->value.real64;
  else
    return 0;
}

const char *named_values_get_string (NamedValues *values, const char *name)
{
  Value *value = hashtable_search (values->table, (void *)name);
    
  if (value && value->type == TYPE_STRING)
    return value->value.str;
  else
    return NULL;
}

Array *named_values_get_opaque (NamedValues *values, const char *name)
{
  Value *value = hashtable_search (values->table, (void *)name);
    
  if (value && value->type == TYPE_OPAQUE)
    return &value->value.bytes;
  else
    return NULL;
}

bool named_values_write (ByteBuffer *buffer, NamedValues *values, 
                         ElvinError *error)
{  
  on_error_return_false 
    (byte_buffer_write_int32 (buffer, hashtable_count (values->table), error));
  
  if (hashtable_count (values->table) > 0)
  {
    struct hashtable_itr *i = hashtable_iterator (values->table);

    do
    {      
      if (byte_buffer_write_string (buffer, hashtable_iterator_key (i), error)) 
        value_write (buffer, hashtable_iterator_value (i), error);
    } while (hashtable_iterator_advance (i) && elvin_error_ok (error));
    
    free (i);
  }
      
  return elvin_error_ok (error);
}

bool named_values_read (ByteBuffer *buffer, NamedValues *values, 
                        ElvinError *error)
{
  uint32_t count;
  char *name;
  Value *value;
  
  for (count = byte_buffer_read_int32 (buffer, error); 
       count > 0 && elvin_error_ok (error); count--)
  {
    if ((name = byte_buffer_read_string (buffer, error)) &&
        (value = value_read (buffer, error)))
    {
      hashtable_insert (values->table, name, value);
    }
  }

  return elvin_error_ok (error);
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
