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

int main (int argc, const char * argv[])
{
  Elvin elvin;
  Attributes *ntfn;
  const char *uri = argc > 1 ? argv [1] : "elvin://localhost";

  if (!elvin_open (&elvin, uri))
  {
    elvin_perror ("open", &elvin.error);
    exit (1);
  }

  ntfn = attributes_create ();

  attributes_set_int32 (ntfn, "favourite number", 42);
  attributes_set_string (ntfn, "some text", "paydirt");

  elvin_send (&elvin, ntfn);

  attributes_destroy (ntfn);

  elvin_close (&elvin);

  if (elvin_error_ok (&elvin.error))
  {
    printf ("Success!\n");

    return 0;
  } else
  {
    elvin_perror ("Elvin", &elvin.error);

    return 1;
  }
}

