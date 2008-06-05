#include <stdio.h>
#include <string.h>

#include "hashtable.h"
#include "hashtable_itr.h"

#include <elvin/values.h>
#include <elvin/named_values.h>

#include "values_private.h"
#include "named_values_private.h"

NamedValues _empty_named_values = {0, NULL, 0, 0, 0, NULL, NULL};

static unsigned int string_hash (void *string);

static int string_equals (void *string1, void *string2);

NamedValues *named_values_init (NamedValues *values)
{
  values = create_hashtable (16, string_hash, string_equals);
  
  return values;
}

void named_values_free (NamedValues *values)
{
  hashtable_destroy (values, 1);
}

unsigned int named_values_size (NamedValues *values)
{
  return hashtable_count (values);
}

void named_values_set (NamedValues *values, const char *name, Value *value)
{
  hashtable_insert (values, strdup (name), value);
}

Value *named_values_get (NamedValues *values, const char *name)
{
  return hashtable_search (values, (void *)name);
}

void named_values_set_int32 (NamedValues *values, const char *name, 
                             int32_t value)
{
  named_values_set (values, name, value_create_int32 (value));
}

int32_t named_values_get_int32 (NamedValues *values, const char *name)
{
  Value *value = hashtable_search (values, (void *)name);
  
  if (value && value->type == TYPE_INT32)
    return value->value.int32;
  else
    return 0;
}

void named_values_set_string (NamedValues *values, 
                              const char *name, const char *value)
{
  named_values_set (values, name, value_create_string (value));
}

const char *named_values_get_string (NamedValues *values, const char *name)
{
  Value *value = hashtable_search (values, (void *)name);
    
  if (value && value->type == TYPE_STRING)
    return value->value.str;
  else
    return NULL;
}

bool named_values_write (ByteBuffer *buffer, NamedValues *values, 
                         ElvinError *error)
{
  struct hashtable_itr *i;
  
  on_error_return_false 
    (byte_buffer_write_int32 (buffer, hashtable_count (values), error));
  
  if (hashtable_count (values) > 0)
  {
    i = hashtable_iterator (values);
    
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
      hashtable_insert (values, name, value);
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
