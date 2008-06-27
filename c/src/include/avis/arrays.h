#ifndef AVIS_ARRAYS_H_
#define AVIS_ARRAYS_H_

/**
 * A generic homogeneous, fixed length array of any type of item.
 */
typedef struct
{
  /** A pointer to the items in the array. */
  void * items;
  
  /** The number of items in the array. */
  size_t item_count; 
} Array;

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

/**
 * Duplicate a block of memory a la strdup ().
 */
void *memdup (const void *source, size_t length);

#endif /*AVIS_ARRAYS_H_*/