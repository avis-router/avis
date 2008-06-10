#include <string.h>
#include <stdio.h>
#include <assert.h>

#include "array_list_private.h"

static void auto_resize (ArrayList *list, size_t min_item_count, 
                         size_t item_size);

ArrayList *array_list_init (ArrayList *list, size_t item_size, 
                            size_t initial_item_count)
{
  list->items_length = initial_item_count * item_size;
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
  int array_list_find_##postfix (ArrayList *list, item_type value)\
  {\
    size_t index;\
    \
    for (index = 0; index < list->item_count; index++)\
    {\
      if (((item_type *)list->items) [index] == value)\
        return (int)index;\
    }\
    \
    return -1;\
  }

def_array_list_find (int, int)
def_array_list_find (void *, ptr)
def_array_list_find (FuncPtr, func)

#define def_array_list_remove(item_type, postfix) \
  bool array_list_remove_##postfix (ArrayList *list, item_type value)\
  {\
    int index = array_list_find_##postfix (list, value);\
    \
    if (index != -1)\
    {\
      array_list_remove (list, index, item_type);\
      \
      return true;\
    } else\
    {\
      return false;\
    }\
  }

def_array_list_remove (int, int)
def_array_list_remove (void *, ptr)
def_array_list_remove (FuncPtr, func)

void array_list_remove_item (ArrayList *list, size_t index, size_t item_size)
{
  assert (index >= 0 && index < list->item_count);
  
  if (index < list->item_count - 1)
  {
    int8_t *target = ((int8_t *)list->items) + (index * item_size);
    
    memmove (target, target + item_size, 
             (list->item_count - index - 1) * item_size);    
  }
  
  list->item_count--;
}

/* TODO use realloc () here */
void auto_resize (ArrayList *list, size_t min_item_count, size_t item_size)
{
  size_t min_length = min_item_count * item_size;
  
  if (list->items_length < min_length)
  {
    void *old_items = list->items;
    
    /* new size is double old size */
    do   
    {
      list->items_length *= 2;
    } while (list->items_length < min_length);
 
    list->items = malloc (list->items_length);
    
    memcpy (list->items, old_items, list->item_count * item_size);
  }
}
