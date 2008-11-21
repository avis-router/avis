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
 * This is the sender part of the security example. In this example a
 * sender uses the Elvin security facility to send messages only to
 * trusted receivers.
 *
 * It requires that you have access to an Elvin router: by default it will
 * use the public router at public.elvin.org. This will likely not work
 * if you're behind a fascist corporate firewall, in which case you could
 * download Avis from http://avis.sourceforge.net and install one locally.
 *
 * Usage: secure_sender [elvin_uri]
 *
 * elvin_uri: optional URI pointing to an Elvin router, e.g
 *
 *   elvin://public.elvin.org
 *   elvin://localhost
 */
int main (int argc, const char *argv [])
{
  const char *uri = argc > 1 ? argv [1] : "elvin://public.elvin.org";
  char password [20];
  char message [128];
  Elvin elvin;
  Attributes *secret_message;
  Keys *sending_keys;
  Key private_key;

  printf ("Enter the password for sending: ");

  if (!fgets (password, sizeof (password), stdin))
    return 1;

  printf ("Enter the message: ");

  if (!fgets (message, sizeof (message), stdin))
    return 1;

  /* Nuke the end of line characters */
  password [strlen (password) - 1] = '\0';
  message [strlen (message) - 1] = '\0';

  secret_message = attributes_create ();
  attributes_set_string (secret_message, "From", "secure-sender");
  attributes_set_string (secret_message, "Message", message);

  /*
   * To ensure only known consumers can receive this message, we use
   * a consumer key scheme, where the producer (this example) uses a
   * public key for its messages and consumers can receive only if
   * they know the private version of the key. It is effectively
   * impossible for untrusted consumers to work out the private key
   * even if they know its public half due to the use of a secure
   * one-way hash such as SHA-1.
   */
  private_key = elvin_key_create_from_string (password);
  sending_keys = elvin_keys_create ();

  /* Add the public key */
  elvin_keys_add
    (sending_keys, KEY_SCHEME_SHA1_CONSUMER,
     elvin_key_create_public (private_key, KEY_SCHEME_SHA1_CONSUMER));

  /* Try to connect, and exit if we fail */
  if (!elvin_open (&elvin, uri))
  {
    elvin_perror ("open", &elvin.error);

    return 1;
  }

  /* Send message, requiring it be delivered securely */
  elvin_send_with_keys
    (&elvin, secret_message, sending_keys, REQUIRE_SECURE_DELIVERY);

  if (elvin_error_occurred (&elvin.error))
  {
    elvin_perror ("send", &elvin.error);

    return 1;
  }

  printf ("Message sent!\n");

  elvin_key_free (private_key);
  elvin_keys_destroy (sending_keys);
  attributes_destroy (secret_message);

  elvin_close (&elvin);

  return 0;
}
