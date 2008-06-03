#include <string.h>
#include <stdio.h>
#include <assert.h>

#include "array_list.h"

static void auto_resize (ArrayList *list, size_t min_item_count);

ArrayList *array_list_init (ArrayList *list, unsigned item_size, 
                            size_t initial_size)
{
  list->items_length = initial_size * item_size;
  list->items = malloc (list->items_length);
  list->item_count = 0;
  list->item_size = item_size;
  
  return list;
}

void array_list_free (ArrayList *list)
{
  if (list->items)
  {
    free (list->items);
    
    list->items = NULL;
    list->item_count = -1;
  }
}

void array_list_add_func (ArrayList *list, void (*func) ())
{
  auto_resize (list, list->item_count + 1);
  
  ((void (**) ())list->items) [list->item_count++] = func;
}

void array_list_add_int (ArrayList *list, int value)
{
  auto_resize (list, list->item_count + 1);
    
  ((int *)list->items) [list->item_count++] = value;
}

int array_list_get_int (ArrayList *list, size_t index)
{
  assert (index >= 0 && index < list->item_count);
  
  return ((int *)list->items) [index];
}

FuncPtr array_list_get_func (ArrayList *list, size_t index)
{
  assert (index >= 0 && index < list->item_count);
  
  return ((void (**) ())list->items) [index];
}

void array_list_remove (ArrayList *list, size_t index)
{
  assert (index >= 0 && index < list->item_count);
  
  if (index < list->item_count - 1)
  {
    int8_t *target = ((int8_t *)list->items) + (index * list->item_size);
    
    memmove (target, target + list->item_size, 
             (list->item_count - index - 1) * list->item_size);    
  }
  
  list->item_count--;
}

int array_list_find_int (ArrayList *list, int int_value)
{
  int index;
  
  for (index = 0; index < list->item_count; index++)
  {
    if (((int *)list->items) [index] == int_value)
      return index;
  }
  
  return -1;
}

int array_list_find_func (ArrayList *list, void (*func) ())
{
  int index;
    
  for (index = 0; index < list->item_count; index++)
  {
    if (((void (**) ())list->items) [index] == func)
      return index;
  }
  
  return -1;
}

bool array_list_remove_int (ArrayList *list, int int_value)
{
  int index = array_list_find_int (list, int_value);
  
  if (index != -1)
  {
    array_list_remove (list, index);
    
    return true;
  } else
  {
    return false;
  }
}

bool array_list_remove_func (ArrayList *list, void (*func) ())
{
  int index = array_list_find_func (list, func);
    
  if (index != -1)
  {
    array_list_remove (list, index);
    
    return true;
  } else
  {
    return false;
  }
}

void auto_resize (ArrayList *list, size_t min_item_count)
{
  size_t min_length = min_item_count * list->item_size;
  
  if (list->items_length < min_length)
  {
    void *old_items = list->items;
    void *new_items;
    
    /* new size is double old size */
    do   
    {
      list->items_length *= 2;
    } while (list->items_length < min_length);
 
    new_items = malloc (list->items_length);
    
    memcpy (new_items, old_items, list->item_count * list->item_size);
    
    list->items = new_items;
  }
}
