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

/*
 * This example demonstrates sending and receiving Elvin notifications
 * with the Elvin C API. See the comments in the source code for details.
 *
 * It requires that you have access to an Elvin router: by default it will
 * use the public router at public.elvin.org. This will likely not work
 * if you're behind a fascist corporate firewall, in which case you could
 * download Avis from http://avis.sourceforge.net and install one locally.
 *
 * Usage: hello_world [elvin_uri]
 *
 * elvin_uri: optional URI pointing to an Elvin router, e.g
 *
 *   elvin://public.elvin.org
 *   elvin://localhost
 */
int main (int argc, const char *argv [])
{
  const char *uri = argc > 1 ? argv [1] : "elvin://public.elvin.org";
  Elvin elvin;
  Attributes *notification;
  Subscription *subscription;

  /* Try to connect, and exit if we fail */
  if (!elvin_open (&elvin, uri))
  {
    elvin_perror ("open", &elvin.error);

    return 1;
  }

  /*
   * Subscribe to notifications with a string Greeting field and an integer
   * Number field.
   *
   * NOTE: we don't have to free the subscription, the connection does
   * that for us on close.
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
   * Start the event loop. This will receive and dispatch messages from the
   * router, starting with the one we just sent. The subscription listener
   * closes the connection after printing the notification, which will
   * cause the event loop to exit.
   */
  elvin_event_loop (&elvin);

  /* This is redundant in this case, but can't hurt. */
  elvin_close (&elvin);

  return 0;
}
