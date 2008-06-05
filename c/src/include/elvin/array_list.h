#ifndef ARRAY_LIST_H_
#define ARRAY_LIST_H_

#include <stdlib.h>

typedef struct
{
  void *items;
  size_t items_length;
  size_t item_count;
} ArrayList;

#endif /*ARRAY_LIST_H_*/
