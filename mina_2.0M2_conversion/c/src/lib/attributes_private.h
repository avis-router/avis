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
#ifndef ATTRIBUTES_PRIVATE_H_
#define ATTRIBUTES_PRIVATE_H_

#include <avis/attributes.h>

#include "byte_buffer.h"

bool attributes_write (ByteBuffer *buffer, Attributes *attributes, 
                       ElvinError *error);

bool attributes_read (ByteBuffer *buffer, Attributes *attributes, 
                      ElvinError *error);

#endif /*ATTRIBUTES_PRIVATE_H_*/
