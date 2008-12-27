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

#include <stdio.h>

#include <avis/elvin.h>

static void close_listener (Elvin *elvin, CloseReason reason,
                            const char *message, void *user_data);
                      
static void sub_listener (Subscription *subscription, Attributes *attributes,
                          bool secure, void *user_data);

void close_listener (Elvin *elvin, CloseReason reason,
                     const char *message, void *user_data)
{
  printf ("Connection closed: %s (%u)\n", message, reason); 
}
                               
void sub_listener (Subscription *subscription, Attributes *attributes,
                   bool secure, void *user_data)
{
  AttributesIter i;
  const char *name;
  const Value *value;
  
  printf ("---\n");

  attributes_iter_init (&i, attributes);

  while (attributes_iter_has_next (&i))
  {
    name = attributes_iter_name (&i);
    value = attributes_iter_value (&i);

    switch (value->type)
    {
    case TYPE_INT32:
      printf ("%s: %i\n", name, value->value.int32);
      break;
    case TYPE_INT64:
      printf ("%s: %liL\n", name, (long int)value->value.int64);
      break;
    case TYPE_REAL64:
      printf ("%s: %f\n", name, value->value.real64);
      break;
    case TYPE_STRING:
      printf ("%s: \"%s\"\n", name, value->value.str);
      break;
    case TYPE_OPAQUE:
      printf ("%s: [%u bytes]\n", name, 
              (unsigned)value->value.bytes.item_count);
      break;
    }

    attributes_iter_next (&i);
  }
}

/*
 * ec is a simple event consumer (ec = Elvin Consumer) that dumps
 * events matching a given subscription to the console.
 * 
 * Usage: ec <router URL> <subscription>
 */
int main (int argc, const char *argv [])
{
  const char *uri = argc == 3 ? argv [1] : "elvin://localhost";
  const char *expr = argc == 3 ? argv [2] : "require (Message)";
  Elvin elvin;
  Subscription *subscription;

  if (expr == NULL)
  {
    printf ("Usage: ec URL subscription\n");

    return 1;
  }

  /* Try to connect, and exit if we fail */
  if (!elvin_open (&elvin, uri))
  {
    elvin_perror ("open", &elvin.error);

    return 2;
  }

  elvin_add_close_listener (&elvin, close_listener, NULL);
  
  subscription = elvin_subscribe (&elvin, expr);

  if (!elvin_error_ok (&elvin.error))
  {
    elvin_perror ("subscribe", &elvin.error);

    return 3;
  }
  
  printf ("Connected to %s\n", uri);

  elvin_subscription_add_listener (subscription, sub_listener, &elvin);

  elvin_event_loop (&elvin);

  elvin_close (&elvin);

  return 0;
}
