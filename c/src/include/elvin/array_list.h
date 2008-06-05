#ifndef ARRAY_LIST_H_
#define ARRAY_LIST_H_

#include <stdlib.h>

typedef struct ArrayList
{
  void *items;
  size_t items_length;
  unsigned item_count;
} ArrayList;

#endif /*ARRAY_LIST_H_*/
