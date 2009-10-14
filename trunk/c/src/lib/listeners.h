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

#ifndef AVIS_LISTENERS_TYPE

  #define AVIS_LISTENERS_TYPE

  typedef ArrayList * Listeners;
#endif

typedef struct
{
  Listener  listener;
  void *    user_data;
} ListenerEntry;

typedef struct
{
  ListenerEntry * entry;
  size_t          remaining;
} ListenersIterator;

#define listeners_init(listeners) {(listeners) = NULL;}

void listeners_free (Listeners *listeners);

#define listeners_iter_init(listeners, i) \
    if ((listeners) == NULL) \
    { \
      (i).entry = NULL; \
      (i).remaining = 0; \
    } else \
    { \
      (i).entry = (listeners)->items; \
      (i).remaining = (listeners)->item_count; \
    }

#define for_each_listener(listeners, l) \
  listeners_iter_init (listeners, l); \
  for ( ; (l).remaining > 0; (l).remaining--, (l).entry++)

void listeners_add (Listeners *listeners, Listener listener, void *user_data);

bool listeners_remove (Listeners *listeners, Listener listener);

#endif /* AVIS_LISTENERS_H_ */
