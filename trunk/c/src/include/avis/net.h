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
/** \file
 * Avis network includes.
 */
#ifndef AVIS_NET_H_
#define AVIS_NET_H_

#ifdef _WIN32
  #include <winsock2.h>
  #include <ws2tcpip.h>

  typedef SOCKET socket_t;

  #define pipe_read(socket, buff, length) recv (socket, buff, length, 0)
  #define pipe_write(socket, buff, length) send (socket, buff, length, 0)

  #define close_socket(s) (closesocket (s), s = -1, WSACleanup ())

  #define sock_op_timed_out() (WSAGetLastError () == WSAETIMEDOUT)

  #define elvin_error_from_pipe elvin_error_from_socket

  #define elvin_error_from_socket(err) \
    elvin_error_set (err, errno_to_elvin_error (WSAGetLastError ()), \
                     "Socket error")
#else
  #include <errno.h>
  #include <unistd.h>
  #include <sys/socket.h>
  #include <netdb.h>
  #include <netinet/in.h>
  #include <arpa/inet.h>

  typedef int socket_t;

  #define pipe_read(socket, buff, length) read (socket, buff, length)
  #define pipe_write(socket, buff, length) write (socket, buff, length)

  #define close_socket(s) (close (s), s = -1)

  #define sock_op_timed_out() (errno == EAGAIN || errno == EWOULDBLOCK)

  #define elvin_error_from_pipe elvin_error_from_errno
  #define elvin_error_from_socket elvin_error_from_errno
#endif

#include <avis/errors.h>
#include <avis/stdtypes.h>
#include <avis/defs.h>

AVIS_PUBLIC
socket_t avis_open_socket (const char *host, uint16_t port, ElvinError *error);

/**
 * Select a socket ready for reading. If both are ready, return socket2.s
 */
AVIS_PUBLIC
socket_t avis_select_ready (socket_t socket1, socket_t socket2, ElvinError *error);

/**
 * Open a bi-directional control socket.
 */
AVIS_PUBLIC
bool avis_open_socket_pair (socket_t *socket_read, socket_t *socket_write,
                          ElvinError *error);

AVIS_PUBLIC
void avis_close_socket_pair (socket_t socket_read, socket_t socket_write);

#endif /* AVIS_NET_H_ */
