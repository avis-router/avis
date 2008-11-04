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
/*
 * Adapted from endian.h in Asterisk (http://www.asterisk.org).
 */

#ifndef AVIS_ENDIAN_H_
#define AVIS_ENDIAN_H_

/*
 * Autodetect system endianess
 */

#ifndef __BYTE_ORDER
  #ifdef __linux__
    #include <endian.h>
  #elif defined (__OpenBSD__) || defined (__FreeBSD__) || \
        defined (__NetBSD__) || defined (__APPLE__)

    #include <machine/endian.h>
    #define __BYTE_ORDER     BYTE_ORDER
    #define __LITTLE_ENDIAN  LITTLE_ENDIAN
    #define __BIG_ENDIAN     BIG_ENDIAN

  #else

    #ifndef __LITTLE_ENDIAN
      #define __LITTLE_ENDIAN   1234
    #endif

    #ifndef __BIG_ENDIAN
      #define __BIG_ENDIAN      4321
    #endif

    #ifdef __LITTLE_ENDIAN__
      #define __BYTE_ORDER __LITTLE_ENDIAN
    #endif /* __LITTLE_ENDIAN */

    #if defined (i386) || defined (__i386__)
      #define __BYTE_ORDER __LITTLE_ENDIAN
    #endif /* defined i386 */

    #if defined (WIN32)
      #define __BYTE_ORDER __LITTLE_ENDIAN
    #endif /* defined WIN32 */

    #if defined (sun) && defined (unix) && defined (sparc)
      #define __BYTE_ORDER __BIG_ENDIAN
    #endif /* sun unix sparc */

  #endif /* linux */

#endif /* __BYTE_ORDER */

#ifndef __BYTE_ORDER
  #error Need to know endianess
#endif

#endif /*AVIS_ENDIAN_H_*/
