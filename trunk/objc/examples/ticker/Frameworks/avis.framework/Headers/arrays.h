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
