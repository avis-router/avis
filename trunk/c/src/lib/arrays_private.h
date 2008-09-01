/*
 *  Avis Elvin client library for C.
 *
 *  Copyright (C) 2008 Matthew Phillips <avis@mattp.name>
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of version 3 of the GNU Lesser General
 *  Public License as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
#ifndef ARRAYS_PRIVATE_H_
#define ARRAYS_PRIVATE_H_

#include <stdlib.h>

#include <avis/stdtypes.h>
#include <avis/arrays.h>

#include "errors_private.h"

typedef void (*FuncPtr) ();

#define array_list_create(item_type, initial_item_count) \
  (array_list_init ((ArrayList *)emalloc (sizeof (ArrayList)), \
   sizeof (item_type), initial_item_count))

#define array_list_destroy(list) \
  (array_list_free (list), free (list), list = NULL)

#define array_list_size(list) ((list)->item_count)

ArrayList *array_list_init (ArrayList *list, size_t item_size,
                            size_t initial_item_count);

void array_list_free (ArrayList *list);

#define array_list_add(list, item_type) \
  ((item_type *)array_list_add_item (list, sizeof (item_type)))

void *array_list_add_item (ArrayList *list, size_t item_size);

void array_list_add_ptr (ArrayList *list, void *item);

void array_list_add_int (ArrayList *list, int value);

void array_list_add_func (ArrayList *list, void (*func) ());

#define array_list_get(list, index, item_type) \
  (((item_type *)list->items) [index])

int array_list_get_int (ArrayList *list, size_t index);

void *array_list_get_ptr (ArrayList *list, size_t index);

FuncPtr array_list_get_func (ArrayList *list, size_t index);

#define array_list_remove(list, index, item_type) \
  (array_list_remove_item (list, index, sizeof (item_type)))

void array_list_remove_item_using_ptr (ArrayList *list, void *item, size_t item_size);

void array_list_remove_item (ArrayList *list, size_t index, size_t item_size);

int32_t *array_list_find_int (ArrayList *list, int32_t int_value);

FuncPtr *array_list_find_func (ArrayList *list, void (*func) ());

void **array_list_find_ptr (ArrayList *list, void *ptr);

bool array_list_remove_int (ArrayList *list, int int_value);

bool array_list_remove_func (ArrayList *list, void (*func) ());

bool array_list_remove_ptr (ArrayList *list, void *ptr);

Array *array_init (Array *array, size_t item_count, size_t item_length);

void array_free (Array *array);

bool array_equals (Array *array1, Array *array2);

Array *array_copy (Array *target, const Array *source, size_t item_size);

#define array_clone(array, item_type) \
  array_copy(emalloc (sizeof (Array)), array, sizeof (item_type))

#endif /*ARRAYS_PRIVATE_H_*/
