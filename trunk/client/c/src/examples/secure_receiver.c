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
 * This is the receiver part of the security example. In this example
 * a sender uses the Elvin security facility to send messages only to
 * trusted receivers.
 *
 * It requires that you have access to an Elvin router: by default it will
 * use the public router at public.elvin.org. This will likely not work
 * if you're behind a fascist corporate firewall, in which case you could
 * download Avis from http://avis.sourceforge.net and install one locally.
 *
 * Usage: secure_receiver [elvin_uri]
 *
 * elvin_uri: optional URI pointing to an Elvin router, e.g
 *
 *   elvin://public.elvin.org
 *   elvin://localhost
 */

void listener (Elvin *elvin, Attributes *attributes,
               bool secure, void *user_data)
{
  printf ("Received message: %s\n",
          attributes_get_string (attributes, "Message"));
}

int main (int argc, const char *argv [])
{
  const char *uri = argc > 1 ? argv [1] : "elvin://public.elvin.org";
  char password [20];
  Elvin elvin;
  Keys *receiving_keys;

  printf ("Enter the password for receiving: ");

  if (!fgets (password, sizeof (password), stdin))
    return 1;

  /* Nuke the end of line character */
  password [strlen (password) - 1] = '\0';

  receiving_keys = elvin_keys_create ();

  /* Add the password as the private key */
  elvin_keys_add (receiving_keys, KEY_SCHEME_SHA1_CONSUMER,
                  elvin_key_create_from_string (password));

  /* Try to connect, and exit if we fail */
  if (!elvin_open (&elvin, uri))
  {
    elvin_perror ("open", &elvin.error);

    return 1;
  }

  /* Subscribe with keys, allowing only secure messages to be received */
  elvin_subscribe_with_keys
    (&elvin, "From == 'secure-sender' && string (Message)",
     receiving_keys, REQUIRE_SECURE_DELIVERY);

  elvin_add_notification_listener (&elvin, listener, NULL);

  printf ("Listening for messages...\n");

  elvin_event_loop (&elvin);

  elvin_keys_destroy (receiving_keys);

  elvin_close (&elvin);

  return 0;
}
