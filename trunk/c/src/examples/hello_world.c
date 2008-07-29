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
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include <avis/elvin.h>

/*
 * Listens for notifications from the router with Message and Number fields.
 */
void sub_listener (Subscription *subscription, Attributes *attributes,
                   bool secure, void *user_data)
{
  printf ("Received greeting: %s\n",
          attributes_get_string (attributes, "Greeting"));

  printf ("     Bonus number: %u\n",
          attributes_get_int32 (attributes, "Number"));

  /* NOTE: it's OK to access client connection from listener callbacks */
  elvin_close (subscription->elvin);
}

/**
 * Usage: hello_world [elvin_uri]
 */
int main (int argc, const char * argv[])
{
  Elvin elvin;
  Attributes *notification;
  Subscription *subscription;
  const char *uri = argc > 1 ? argv [1] : "elvin://localhost";

  /* Exit if we failed to connect for any reason */
  if (!elvin_open (&elvin, uri))
  {
    elvin_perror ("open", &elvin.error);

    return 1;
  }

  /*
   * Subscribe to notifications with a string Greeting field and an integer
   * Number field.
   */
  subscription =
    elvin_subscribe (&elvin, "string (Greeting) && int32 (Number)");

  elvin_subscription_add_listener (subscription, sub_listener, &elvin);

  /* Send a notification that our subscription matches. */
  notification = attributes_create ();

  attributes_set_string (notification, "Greeting", "Hello World");
  attributes_set_int32  (notification, "Number", 42);

  elvin_send (&elvin, notification);

  attributes_destroy (notification);

  /*
   * Run event loop. This will receive and dispatch messages from the
   * router, including the one we just sent. The subscription listener
   * closes the connection after printing the notification, which will
   * cause the event loop to exit.
   */
  elvin_event_loop (&elvin);

  /** This is redundant in this case, but good practice. */
  elvin_close (&elvin);

  return 0;
}
