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
#include <avis/attributes.h>

void sub_listener (Subscription *sub, Attributes *attributes, bool secure, 
                   void *user_data);

int main (int argc, const char * argv[]) 
{
  Elvin elvin;
  Subscription *sub;
  ElvinError error = ELVIN_EMPTY_ERROR;
  const char *uri = argc > 1 ? argv [1] : "elvin://localhost";
  
  if (!elvin_open (&elvin, uri, &error))
  {
    elvin_perror ("open", &error);
    exit (1);
  }
  
  /* TODO handle Ctrl+C */
  sub = elvin_subscribe (&elvin, "require (test) && string (message)", &error);
  
  if (!sub)
  {
    elvin_perror ("subscribe", &error);
    exit (1);
  }
  
  elvin_subscription_add_listener (sub, sub_listener, NULL);
  
  while (elvin_is_open (&elvin) && elvin_error_ok (&error))
  {
    elvin_poll (&elvin, &error);
  }
  
  if (elvin_error_occurred (&error))
    elvin_perror ("receive", &error);
  
  elvin_close (&elvin);
  
  return 0;
}

void sub_listener (Subscription *sub, Attributes *attributes, bool secure, 
                   void *user_data)
{
  printf ("Notified! Message = %s\n", 
          attributes_get_string (attributes, "message"));
}
