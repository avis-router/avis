/** \file
 * Variable length arrays. Not for client use.
 */
#ifndef ARRAY_LIST_H_
#define ARRAY_LIST_H_

#include <stdlib.h>

/**
 * A variable-length array. This is used internally and is not intended for
 * client use.
 */
typedef struct
{
  void * items;
  size_t items_length;
  size_t item_count;
} ArrayList;

#endif /*ARRAY_LIST_H_*/
