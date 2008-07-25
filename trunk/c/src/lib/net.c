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

  static bool init_windows_sockets (ElvinError *error);

  static int dumb_socketpair (SOCKET socks [2], int make_overlapped);
#endif

#define init_timeout(t) {(t) / 1000, ((t) % 1000) * 1000}

#define max(a,b) (a > b ? a : b)

socket_t select_ready (socket_t socket1, socket_t socket2, ElvinError *error)
{
  fd_set socks;
  int ready_sockets;

  FD_ZERO (&socks);
  FD_SET (socket1, &socks);
  FD_SET (socket2, &socks);

  ready_sockets =
    select (max (socket1, socket2) + 1, &socks, NULL, NULL, NULL);

  if (ready_sockets == 0)
  {
    elvin_error_from_socket (error);

    return -1;
  } else if (ready_sockets == 1)
  {
    return (FD_ISSET (socket1, &socks)) ? socket1 : socket2;
  } else
  {
    return socket2;
  }
}

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
        struct timeval timeout = init_timeout (AVIS_IO_TIMEOUT);

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
    elvin_error_from_socket (error);

  return sock;
}

bool open_socket_pair (socket_t *socket_read, socket_t *socket_write,
                       ElvinError *error)
{
  #ifdef WIN32

    SOCKET sockets [2];

    if (!init_windows_sockets (error))
      return -1;

    if (!dumb_socketpair (sockets, 0))
    {
      *socket_read = sockets [0];
      *socket_write = sockets [1];

      return true;
    } else
    {
      return false;
    }

  #else

    int pipes [2];

    if (pipe (pipes) == 0)
    {
      *socket_read = pipes [0];
      *socket_write = pipes [1];

      return true;
    } else
    {
      return elvin_error_from_errno (error);
    }

  #endif /* defined (WIN32) */
}

void close_socket_pair (socket_t socket_read, socket_t socket_write)
{
  #ifdef WIN32

    closesocket (socket_read);
    closesocket (socket_write);

    WSACleanup ();

  #else

    close (socket_read);
    close (socket_write);

  #endif
}

#ifdef WIN32

#include <windows.h>
#include <io.h>

/*
 * See http://cantrip.org/socketpair.c
 */

/* socketpair.c
 * Copyright 2007 by Nathan C. Myers <ncm@cantrip.org>; all rights reserved.
 * This code is Free Software.  It may be copied freely, in original or
 * modified form, subject only to the restrictions that (1) the author is
 * relieved from all responsibilities for any use for any purpose, and (2)
 * this copyright notice must be retained, unchanged, in its entirety.  If
 * for any reason the author might be held responsible for any consequences
 * of copying or use, license is withheld.
 */

/* dumb_socketpair:
 *   If make_overlapped is nonzero, both sockets created will be usable for
 *   "overlapped" operations via WSASend etc.  If make_overlapped is zero,
 *   socks[0] (only) will be usable with regular ReadFile etc., and thus
 *   suitable for use as stdin or stdout of a child process.  Note that the
 *   sockets must be closed with closesocket() regardless.
 */
int dumb_socketpair (SOCKET socks [2], int make_overlapped)
{
  struct sockaddr_in addr;
  SOCKET listener;
  int e;
  int addrlen = sizeof (addr);
  DWORD flags = (make_overlapped ? WSA_FLAG_OVERLAPPED : 0);

  if (socks == 0)
  {
    WSASetLastError (WSAEINVAL);

    return SOCKET_ERROR;
  }

  socks [0] = socks [1] = INVALID_SOCKET;

  if ((listener = socket (AF_INET, SOCK_STREAM, 0)) == INVALID_SOCKET)
    return SOCKET_ERROR;

  memset(&addr, 0, sizeof (addr));
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = htonl (0x7f000001);
  addr.sin_port = 0;

  e = bind (listener, (const struct sockaddr*) &addr, sizeof (addr));

  if (e == SOCKET_ERROR)
  {
    e = WSAGetLastError ();
    closesocket (listener);
    WSASetLastError (e);

    return SOCKET_ERROR;
  }
  e = getsockname (listener, (struct sockaddr*) &addr, &addrlen);

  if (e == SOCKET_ERROR)
  {
    e = WSAGetLastError ();
    closesocket (listener);
    WSASetLastError (e);

    return SOCKET_ERROR;
  }

  do
  {
    if (listen (listener, 1) == SOCKET_ERROR) break;

    if ((socks [0] = WSASocket(AF_INET, SOCK_STREAM, 0, NULL, 0, flags))
        == INVALID_SOCKET) break;
    if (connect (socks [0], (const struct sockaddr*) &addr,
            sizeof (addr)) == SOCKET_ERROR) break;

    if ((socks [1] = accept (listener, NULL, NULL))
        == INVALID_SOCKET) break;

    closesocket (listener);

    return 0;
  } while (0);

  e = WSAGetLastError();

  closesocket (listener);
  closesocket (socks [0]);
  closesocket (socks [1]);

  WSASetLastError (e);

  return SOCKET_ERROR;
}

bool init_windows_sockets (ElvinError *error)
{
  WSADATA wsaData;
  int err;

  err = WSAStartup (MAKEWORD (2, 2), &wsaData);

  if (err != 0)
  {
    return elvin_error_set (error, ELVIN_ERROR_INTERNAL,
                            "Failed to init winsock library");
  } else if (LOBYTE (wsaData.wVersion) != 2 ||
             HIBYTE (wsaData.wVersion) != 2)
  {
    WSACleanup ();

    return elvin_error_set (error, ELVIN_ERROR_INTERNAL,
                            "Failed to find winsock 2.2");
  } else
  {
    return true;
  }
}

#endif
