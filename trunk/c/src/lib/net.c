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
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <avis/defs.h>
#include <avis/net.h>

#ifdef WIN32
  #define snprintf _snprintf

  static int init_windows_sockets (ElvinError *error);
#endif

socket_t open_socket (const char *host, uint16_t port, ElvinError *error)
{
  struct addrinfo hints;
  struct addrinfo *info;
  struct addrinfo *i;
  int error_code;
  socket_t sock = -1;
  char service [10];

  #ifdef WIN32
    if (!init_windows_sockets (error))
      return -1;
  #endif

  snprintf (service, sizeof (service), "%u", port);

  memset (&hints, 0, sizeof (hints));
  hints.ai_family = PF_UNSPEC;
  hints.ai_socktype = SOCK_STREAM;

  if ((error_code = getaddrinfo (host, service, &hints, &info)))
  {
    elvin_error_set (error, host_to_elvin_error (error_code),
                     gai_strerror (error_code));

    return -1;
  }

  for (i = info; i && sock == -1; i = i->ai_next)
  {
    sock = socket (i->ai_family, SOCK_STREAM, 0);

    if (sock != -1)
    {
      if (connect (sock, i->ai_addr, i->ai_addrlen) == 0)
      {
        /* set send/receive timeouts */
        #ifdef WIN32
          /*
           * Windows seems to treat the seconds field as
           * milliseconds from what I can see running tests.
           * Lord only knows what it thinks the microseconds
           * field is.
           */
          struct timeval timeout = {AVIS_IO_TIMEOUT, 0};
        #else
          struct timeval timeout =
            {AVIS_IO_TIMEOUT / 1000, (AVIS_IO_TIMEOUT % 1000) * 1000};
        #endif

        setsockopt (sock, SOL_SOCKET, SO_RCVTIMEO,
                    (char *)&timeout, sizeof (timeout));
        setsockopt (sock, SOL_SOCKET, SO_SNDTIMEO,
                    (char *)&timeout, sizeof (timeout));
      } else
      {
        close_socket (sock);

        sock = -1;
      }
    }
  }

  freeaddrinfo (info);

  if (sock == -1)
    elvin_error_from_errno (error);

  return sock;
}

#ifdef WIN32

int init_windows_sockets (ElvinError *error)
{
  WSADATA wsaData;
  int err;

  err = WSAStartup (MAKEWORD (2, 2), &wsaData);

  if (err != 0)
  {
    elvin_error_set (error, ELVIN_ERROR_INTERNAL,
                     "Failed to init winsock library");
	return 0;
  } else if (LOBYTE (wsaData.wVersion) != 2 ||
             HIBYTE (wsaData.wVersion) != 2)
  {
    WSACleanup ();

    elvin_error_set (error, ELVIN_ERROR_INTERNAL,
                     "Failed to find winsock 2.2");
	return 0;
  }
  return 1;
}

#endif
