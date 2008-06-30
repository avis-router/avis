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
#ifndef AVIS_LISTENERS_H_
#define AVIS_LISTENERS_H_

#include "arrays_private.h"

typedef void (*Listener) ();

#ifndef AVIS_LISTENERS_STRUCT

  #define AVIS_LISTENERS_STRUCT

  typedef struct Listeners
  {
    ArrayList *list;
  } Listeners;
#endif

typedef struct
{
  Listener  listener;
  void *    user_data;
} ListenerEntry;

typedef struct
{
  ListenerEntry * entry;
  int             remaining;
} ListenersIter;

#define listeners_init(listeners) {(listeners)->list = NULL;}

void listeners_free (Listeners *listeners);

#define listeners_iter_start(listeners, i) \
    if (listeners.list == NULL) \
    { \
      (i).entry = NULL; \
      (i).remaining = 0; \
    } else \
    { \
      (i).entry = listeners.list->items; \
      (i).remaining = listeners.list->item_count; \
    }

#define each_listener(l) \
  for ( ; l.remaining > 0; l.remaining--, l.entry++)

void listeners_add (Listeners *listeners, Listener listener, void *user_data);

bool listeners_remove (Listeners *listeners, Listener listener);

#endif /* AVIS_LISTENERS_H_ */
