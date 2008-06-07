#include <stdio.h>
#include <string.h>

#include "hashtable.h"
#include "hashtable_itr.h"

#include <avis/values.h>
#include <avis/attributes.h>

#include "values_private.h"
#include "attributes_private.h"

static struct hashtable empty_hashtable = {0, NULL, 0, 0, 0, NULL, NULL};

Attributes _empty_attributes = {&empty_hashtable};

static unsigned int string_hash (void *string);

static int string_equals (void *string1, void *string2);

Attributes *attributes_init (Attributes *attributes)
{
  attributes->table = malloc (sizeof (struct hashtable));
  
  init_hashtable (attributes->table, 16, string_hash, string_equals);
  
  return attributes;
}

void attributes_free (Attributes *attributes)
{
  /* free entries */
  if (hashtable_count (attributes->table) > 0)
  {
    struct hashtable_itr *i = hashtable_iterator (attributes->table);

    do
    {      
      free (hashtable_iterator_key (i));
      
      value_destroy (hashtable_iterator_value (i));
    } while (hashtable_iterator_advance (i));
    
    free (i);
  }

  hashtable_free (attributes->table, 0);
}

unsigned int attributes_size (Attributes *attributes)
{
  return hashtable_count (attributes->table);
}

void attributes_set (Attributes *attributes, const char *name, Value *value)
{
  Value *old_value = attributes_remove (attributes, name);
  
  if (old_value)
    value_destroy (old_value);
  
  hashtable_insert (attributes->table, strdup (name), value);
}

Value *attributes_get (Attributes *attributes, const char *name)
{
  return hashtable_search (attributes->table, (void *)name);
}

Value *attributes_remove (Attributes *attributes, const char *name)
{
  return hashtable_remove (attributes->table, (void *)name);
}

int32_t attributes_get_int32 (Attributes *attributes, const char *name)
{
  Value *value = hashtable_search (attributes->table, (void *)name);
  
  if (value && value->type == TYPE_INT32)
    return value->value.int32;
  else
    return 0;
}

int64_t attributes_get_int64 (Attributes *attributes, const char *name)
{
  Value *value = hashtable_search (attributes->table, (void *)name);
  
  if (value && value->type == TYPE_INT64)
    return value->value.int64;
  else
    return 0;
}

real64_t attributes_get_real64 (Attributes *attributes, const char *name)
{
  Value *value = hashtable_search (attributes->table, (void *)name);
  
  if (value && value->type == TYPE_REAL64)
    return value->value.real64;
  else
    return 0;
}

const char *attributes_get_string (Attributes *attributes, const char *name)
{
  Value *value = hashtable_search (attributes->table, (void *)name);
    
  if (value && value->type == TYPE_STRING)
    return value->value.str;
  else
    return NULL;
}

Array *attributes_get_opaque (Attributes *attributes, const char *name)
{
  Value *value = hashtable_search (attributes->table, (void *)name);
    
  if (value && value->type == TYPE_OPAQUE)
    return &value->value.bytes;
  else
    return NULL;
}

bool attributes_write (ByteBuffer *buffer, Attributes *attributes, 
                         ElvinError *error)
{  
  on_error_return_false 
    (byte_buffer_write_int32 (buffer, hashtable_count (attributes->table), error));
  
  if (hashtable_count (attributes->table) > 0)
  {
    struct hashtable_itr *i = hashtable_iterator (attributes->table);

    do
    {      
      if (byte_buffer_write_string (buffer, hashtable_iterator_key (i), error)) 
        value_write (buffer, hashtable_iterator_value (i), error);
    } while (hashtable_iterator_advance (i) && elvin_error_ok (error));
    
    free (i);
  }
      
  return elvin_error_ok (error);
}

bool attributes_read (ByteBuffer *buffer, Attributes *attributes, 
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
      hashtable_insert (attributes->table, name, value);
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
