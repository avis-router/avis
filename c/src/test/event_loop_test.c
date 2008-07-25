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

#ifdef WIN32
  typedef HANDLE thread_t;

  #define create_thread(thread, handler, param) \
    ((thread = CreateThread (NULL, 0, handler, param, 0, NULL)) == NULL)

  #define sleep(secs) Sleep (secs * 1000)

  #define decl_thread_proc(name, param) DWORD WINAPI name (LPVOID param)
#else
  #include <unistd.h>
  #include <pthread.h>

  typedef pthread_t thread_t;
  #define create_thread(thread, handler, param) pthread_create (&thread, NULL, handler, param)

  #define decl_thread_proc(name, param) void *name (void *param)
#endif

void close_listener (Elvin *elvin, CloseReason reason, const char *message,
                     void *user_data)
{
  printf ("Connection closed: %s\n", message);
}

void sub_listener (Subscription *sub, Attributes *attributes, bool secure,
                   void *user_data)
{
  printf ("Notified! Message = %s\n",
          attributes_get_string (attributes, "message"));
}

static decl_thread_proc (close_thread_main, data)
{
  sleep (2);

  printf ("Closing connection on timeout\n");

  elvin_close ((Elvin *)data);

  return 0;
}

int main (int argc, const char * argv[])
{
  Elvin elvin;
  Subscription *sub;
  ElvinError error = ELVIN_EMPTY_ERROR;
  const char *uri = argc > 1 ? argv [1] : "elvin://localhost";
  thread_t close_thread;

  if (!elvin_open (&elvin, uri, &error))
  {
    elvin_perror ("open", &error);
    exit (1);
  }

  elvin_add_close_listener (&elvin, close_listener, NULL);

  if (create_thread (close_thread, close_thread_main, &elvin))
    exit (1);

  /* TODO handle Ctrl+C */
  sub = elvin_subscribe (&elvin, "require (test) && string (message)", &error);

  if (!sub)
  {
    elvin_perror ("subscribe", &error);
    exit (1);
  }

  elvin_subscription_add_listener (sub, sub_listener, NULL);

  printf ("Start event loop...\n");
  elvin_event_loop (&elvin, &error);
  printf ("End event loop\n");

  if (elvin_error_occurred (&error))
    elvin_perror ("receive", &error);

  elvin_close (&elvin);

  return 0;
}
