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
#ifndef ERRORS_PRIVATE_H_
#define ERRORS_PRIVATE_H_

#include <avis/errors.h>

#define error_fail(message) avis_fail ((message), __FILE__, __LINE__)

#define emalloc(size) do_avis_emalloc ((size), __FILE__, __LINE__)

#define estrdup(str) do_avis_estrdup ((str), __FILE__, __LINE__)

#define check_max_size(value, max, message, error) \
  if ((value) > (max)) \
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, message);

AVIS_PUBLIC
void avis_fail (const char *message, const char *file, int line, ...);

#endif /* ERRORS_PRIVATE_H_ */
