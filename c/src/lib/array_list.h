#ifndef ARRAY_LIST_H_
#define ARRAY_LIST_H_

#include <stdlib.h>

#include <elvin/stdtypes.h>

typedef void (*FuncPtr) ();

typedef struct ArrayList
{
  void *items;
  size_t items_length;
  size_t item_count;
} ArrayList;

#define array_list_create(item_size, initial_size) \
  (array_list_init (malloc (sizeof (ArrayList)), item_size, initial_size))

#define array_list_destroy(list) \
  (array_list_free (list), free (list), list = NULL)

#define array_list_size(list) ((list)->item_count)

ArrayList *array_list_init (ArrayList *list, size_t item_size, 
                            size_t initial_item_count);

void array_list_free (ArrayList *list);

void array_list_add_int (ArrayList *list, int value);

void array_list_add_func (ArrayList *list, void (*func) ());

int array_list_get_int (ArrayList *list, size_t index);

FuncPtr array_list_get_func (ArrayList *list, size_t index);

#define array_list_remove(list, index, item_type) \
  (array_list_remove_item (list, index, sizeof (item_type)))

void array_list_remove_item (ArrayList *list, size_t index, size_t item_size);

int array_list_find_int (ArrayList *list, int int_value);

int array_list_find_func (ArrayList *list, void (*func) ());

bool array_list_remove_int (ArrayList *list, int int_value);

bool array_list_remove_func (ArrayList *list, void (*func) ());

#endif /*ARRAY_LIST_H_*/
