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

void array_list_add_ptr (ArrayList *list, void *item)
{
  auto_resize (list, list->item_count + 1, sizeof (void *));
  
  ((void **)list->items) [list->item_count++] = item;
}

void array_list_add_func (ArrayList *list, FuncPtr func)
{
  auto_resize (list, list->item_count + 1, sizeof (FuncPtr));
  
  ((FuncPtr *)list->items) [list->item_count++] = func;
}

void array_list_add_int (ArrayList *list, int value)
{
  auto_resize (list, list->item_count + 1, sizeof (int));
    
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

void *array_list_get_ptr (ArrayList *list, size_t index)
{
  assert (index >= 0 && index < list->item_count);
  
  return ((void **)list->items) [index];
}

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

int array_list_find_int (ArrayList *list, int int_value)
{
  size_t index;
  
  for (index = 0; index < list->item_count; index++)
  {
    if (((int *)list->items) [index] == int_value)
      return (int)index;
  }
  
  return -1;
}

int array_list_find_func (ArrayList *list, void (*func) ())
{
  size_t index;
    
  for (index = 0; index < list->item_count; index++)
  {
    if (((void (**) ())list->items) [index] == func)
      return (int)index;
  }
  
  return -1;
}

int array_list_find_ptr (ArrayList *list, void *ptr)
{
  size_t index;
    
  for (index = 0; index < list->item_count; index++)
  {
    if (((void **)list->items) [index] == ptr)
      return (int)index;
  }
  
  return -1;
}

bool array_list_remove_int (ArrayList *list, int int_value)
{
  int index = array_list_find_int (list, int_value);
  
  if (index != -1)
  {
    array_list_remove (list, index, int);
    
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
    array_list_remove (list, index, FuncPtr);
    
    return true;
  } else
  {
    return false;
  }
}

bool array_list_remove_ptr (ArrayList *list, void *ptr)
{
  int index = array_list_find_ptr (list, ptr);
      
  if (index != -1)
  {
    array_list_remove (list, index, void *);
    
    return true;
  } else
  {
    return false;
  }
}

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
