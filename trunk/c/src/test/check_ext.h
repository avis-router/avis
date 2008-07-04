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
#ifndef CHECK_EXT_H_
#define CHECK_EXT_H_

#include <avis/errors.h>

#include "errors_private.h"

#define fail_on_error(error) \
  (fail_unless ((error)->code == ELVIN_ERROR_NONE, \
      "Elvin error: %s", (error)->message))

#define fail_unless_error_code(error,expected_code) \
  (fail_unless ((error)->code == (expected_code), \
      "Expected elvin error"), elvin_error_reset (error))

#endif /*CHECK_EXT_H_*/
