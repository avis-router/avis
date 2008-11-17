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
 * Elvin global value definitions.
 */
#ifndef AVIS_DEFS_H_
#define AVIS_DEFS_H_

/** The default port for Elvin client connections. */
#define DEFAULT_ELVIN_PORT 2917

/** The default client protocol major version supported by this library. */
#define DEFAULT_CLIENT_PROTOCOL_MAJOR 4

/** The default client protocol minor version supported by this library. */
#define DEFAULT_CLIENT_PROTOCOL_MINOR 0

/** Timeout (in milliseconds) for I/O operations. */
#define AVIS_IO_TIMEOUT 10000

#define _KB_  1024
#define _MB_  _KB_ * _KB_

/**
 * Max total length in bytes of an acceptable packet from an Elvin router.
 */
#define MAX_PACKET_LENGTH    (2 * _MB_)

/**
 * Max total length in bytes of an acceptable UTF-8 string from an Elvin
 * router.
 */
#define MAX_STRING_LENGTH    (1 * _MB_)

/**
 * Max total length in bytes of an acceptable opaque byte array from an
 * Elvin router.
 */
#define MAX_OPAQUE_LENGTH    (1 * _MB_)

/**
 * Max total length of an acceptable array (of int64 or polymorphic values)
 * from an Elvin router.
 */
#define MAX_ARRAY_LENGTH     (4 * _KB_)

/**
 * Max total number of entries in an attributes set from an Elvin router.
 */
#define MAX_ATTRIBUTE_COUNT  (4 * _KB_)

/**
 * Max total number of key scheme/key set pairs from an Elvin router. This
 * currently has no effect on the client since it only sends keys not receives
 * them, but might be useful if this library ever forms the basis of a server.
 */
#define MAX_KEY_SCHEME_COUNT 16

/**
 * Max total number of keys in a key set from an Elvin router. This currently
 * has no effect on the client since it only sends keys not receives them, but
 * might be useful if this library ever forms the basis of a server.
 */
#define MAX_KEY_COUNT  (1 * _KB_)

#ifdef WIN32
#  if !defined (AVIS_LIBRARY_STATIC)
#    if defined (AVIS_BUILDING_LIB)
#      define AVIS_PUBLIC       __declspec(dllexport)
#      define AVIS_PUBLIC_DATA  extern __declspec(dllexport)
#    else
#      define AVIS_PUBLIC       __declspec(dllimport)
#      define AVIS_PUBLIC_DATA  extern __declspec(dllimport)
#    endif
#  else
    /* Static links must use extern */
#    define AVIS_PUBLIC        extern
#    define AVIS_PUBLIC_DATA   extern
#  endif
#else
#  define AVIS_PUBLIC
#  define AVIS_PUBLIC_DATA   extern
#endif

#endif /* AVIS_DEFS_H_ */
