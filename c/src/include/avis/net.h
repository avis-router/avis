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

#ifdef WIN32
  #include <winsock2.h>
  #include <ws2tcpip.h>

  typedef SOCKET socket_t;

  #define close_socket(s) closesocket (s)

  #define sock_op_timed_out() (WSAGetLastError () == WSAETIMEDOUT)

  #define elvin_error_from_socket(err) \
    elvin_error_set (err, errno_to_elvin_error (WSAGetLastError ()), \
                     "Socket error")
#else
  #include <sys/socket.h>
  #include <netdb.h>
  #include <netinet/in.h>
  #include <arpa/inet.h>

  typedef int socket_t;

  #define close_socket(s) close (s)

  #define sock_op_timed_out() (errno == EAGAIN || errno == EWOULDBLOCK)

  #define elvin_error_from_socket(err) elvin_error_from_errno (err)
#endif

#endif /* AVIS_NET_H_ */
