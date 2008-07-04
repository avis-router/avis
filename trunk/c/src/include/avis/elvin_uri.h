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
 * Elvin URI handling.
 */
#ifndef AVIS_ELVIN_URI_H_
#define AVIS_ELVIN_URI_H_

#include <avis/stdtypes.h>
#include <avis/errors.h>

/**
 * A URI referring to an Elvin router endpoint.
 *
 * @see elvin_uri_from_string()
 * @see elvin_open_uri().
 */
typedef struct
{
  char *   host;
  uint16_t port;
  uint16_t version_major;
  uint16_t version_minor;
} ElvinURI;

/**
 * Parse a URI pointing to an Elvin router endpoint.
 *
 * @param uri The URI to initialise.
 * @param uri_string The URI in text form.
 * @param error The error info.
 *
 * Example URI's:
 *
 * <pre>
 * elvin://host
 * elvin://host:port
 * elvin:/xdr,none,ssl/host:port
 * elvin:4.1/xdr,none,ssl/host:port
 * elvin:4.1/xdr,none,ssl/host:port?n1=v1;n2=v2
 * </pre>
 *
 * @see elvin_uri_free()
 */
bool elvin_uri_from_string (ElvinURI *uri, const char *uri_string,
                            ElvinError *error);

/**
 * Free any resources allocated to a URI.
 */
void elvin_uri_free (ElvinURI *uri);

#endif /*AVIS_ELVIN_URI_H_*/
