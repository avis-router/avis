#include <stdio.h>
#include <string.h>

#include "hashtable.h"
#include <hashtable_itr.h>

#include <elvin/values.h>
#include <elvin/named_values.h>

#include "values_private.h"
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
  const char *name;
  Value *value;
  struct hashtable_itr *i;
  
  on_error_return_false 
    (byte_buffer_write_int32 (buffer, named_values_size (values), error));
  
  if (hashtable_count (values->table) > 0)
  {
    i = hashtable_iterator (values->table);
    
    do
    {
      name = hashtable_iterator_key (i);
      value = hashtable_iterator_value (i);
      
      byte_buffer_write_string (buffer, name, error) 
      &&
      value_write (buffer, value, error);
    } while (elvin_error_ok (error) && hashtable_iterator_advance (i));
    
    free (i);
  }
      
  return elvin_error_ok (error);
}

bool named_values_read (ByteBuffer *buffer, NamedValues *values, 
                        ElvinError *error)
{
  uint32_t count;
  const char *name;
  Value *value;
  
  count = byte_buffer_read_int32 (buffer, error);
  
  for ( ; elvin_error_ok (error) && count > 0; count--)
  {
    (name = byte_buffer_read_string (buffer, error)) 
    &&
    (value = value_read (buffer, error));
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
