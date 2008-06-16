#include <string.h>
#include <stdio.h>
#include <assert.h>
#include <math.h>

#include "array_list_private.h"

#define max(x,y) ((x) > (y) ? (x) : (y))

static void auto_resize (ArrayList *list, size_t min_item_count, 
                         size_t item_size);

ArrayList *array_list_init (ArrayList *list, size_t item_size, 
                            size_t initial_item_count)
{
  list->items_length = max (initial_item_count, 1) * item_size;
  list->items = malloc (list->items_length);
  list->item_count = 0;
  
  return list;
}

void array_list_free (ArrayList *list)
{
  if (list->items)
  {
    free (list->items);
    
    list->items = NULL;
    list->item_count = 0;
  }
}

void *array_list_add_item (ArrayList *list, size_t item_size)
{
  auto_resize (list, list->item_count + 1, item_size);

  return (uint8_t *)list->items + (list->item_count++ * item_size);
}

/*
 * Matthew's Poor Man's Generics (tm) below
 */

#define def_array_list_add(item_type, postfix) \
  void array_list_add_##postfix (ArrayList *list, item_type item)\
  {\
    auto_resize (list, list->item_count + 1, sizeof (item_type));\
    \
    ((item_type *)list->items) [list->item_count++] = item;\
  }

def_array_list_add (int, int)
def_array_list_add (void *, ptr)
def_array_list_add (FuncPtr, func)

#define def_array_list_get(item_type, postfix) \
  item_type array_list_get_##postfix (ArrayList *list, size_t index)\
  {\
    assert (index >= 0 && index < list->item_count);\
    \
    return ((item_type *)list->items) [index];\
  }\
  
def_array_list_get (int, int)
def_array_list_get (void *, ptr)
def_array_list_get (FuncPtr, func)

#define def_array_list_find(item_type, postfix) \
  item_type *array_list_find_##postfix (ArrayList *list, item_type value)\
  {\
    item_type *ptr; \
    int index = list->item_count; \
    \
    for (ptr = list->items; index > 0; index--, ptr++) \
    { \
      if (*ptr == value) \
        return ptr;\
    } \
    \
    return NULL; \
  }

def_array_list_find (int32_t, int)
def_array_list_find (void *, ptr)
def_array_list_find (FuncPtr, func)

#define def_array_list_remove(item_type, postfix) \
  bool array_list_remove_##postfix (ArrayList *list, item_type value)\
  {\
    item_type *ptr = array_list_find_##postfix (list, value);\
    \
    if (ptr)\
    {\
      array_list_remove_item_using_ptr (list, ptr, sizeof (item_type));\
      \
      return true;\
    } else\
    {\
      return false;\
    }\
  }

def_array_list_remove (int32_t, int)
def_array_list_remove (void *, ptr)
def_array_list_remove (FuncPtr, func)

void array_list_remove_item (ArrayList *list, size_t index, size_t item_size)
{
  assert (index >= 0 && index < list->item_count);
  
  array_list_remove_item_using_ptr 
    (list, (int8_t *)list->items + (index * item_size), item_size);
}

void array_list_remove_item_using_ptr (ArrayList *list, void *item, 
                                       size_t item_size)
{
  int8_t *end = (int8_t *)list->items + (list->item_count * item_size);
  int8_t *src = (int8_t *)item + item_size;
  
  assert (list->item_count > 0);
  
  if ((int8_t *)item < end - item_size)
    memmove (item, src, end - src);    
  
  list->item_count--;
}

void auto_resize (ArrayList *list, size_t min_item_count, size_t item_size)
{
  size_t min_length = min_item_count * item_size;
  
  if (list->items_length < min_length)
  {
    /* new size is double old size */
    do   
    {
      list->items_length *= 2;
    } while (list->items_length < min_length);
 
    list->items = realloc (list->items, list->items_length);
    
    if (!list->items)
    {
      /* TODO what is the best thing to do when we run out of memory? */
      abort ();
    }
  }
}
