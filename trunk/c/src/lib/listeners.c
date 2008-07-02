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
#include "listeners.h"

void listeners_free (Listeners *listeners)
{
  if (*listeners != NULL)
    array_list_destroy (*listeners);
}

void listeners_add (Listeners *listeners, Listener listener, void *user_data)
{
  ListenerEntry *entry;

  if (*listeners == NULL)
    *listeners = array_list_create (ListenerEntry, 2);

  entry = array_list_add (*listeners, ListenerEntry);

  entry->listener = listener;
  entry->user_data = user_data;
}

bool listeners_remove (Listeners *listeners, Listener listener)
{
  ListenerEntry *entry;
  int count;

  if (*listeners == NULL)
    return false;

  entry = (*listeners)->items;

  for (count = (*listeners)->item_count;
       count > 0 && entry->listener != listener; count--, entry++);

  if (count > 0)
  {
    array_list_remove_item_using_ptr (*listeners, entry,
                                      sizeof (ListenerEntry));

    if ((*listeners)->item_count == 0)
      array_list_destroy (*listeners);

    return true;
  } else
  {
    return false;
  }
}
