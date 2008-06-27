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
#ifndef KEYS_PRIVATE_H_
#define KEYS_PRIVATE_H_

#include <avis/errors.h>
#include <avis/keys.h>

#include "byte_buffer.h"

bool elvin_keys_read (ByteBuffer *buffer, Keys *keys, ElvinError *error);

bool elvin_keys_write (ByteBuffer *buffer, Keys *keys, ElvinError *error);

#endif /*KEYS_PRIVATE_H_*/
