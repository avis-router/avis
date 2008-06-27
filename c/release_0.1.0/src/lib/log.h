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
 * Logging support.
 */
#ifndef AVIS_LOG_H_
#define AVIS_LOG_H_

#define ELVIN_LOG_LEVEL       1

#define LOG_LEVEL_DIAGNOSTIC  5
#define LOG_LEVEL_TRACE       6

#define LOGGING(loglevel) (ELVIN_LOG_LEVEL >= loglevel)

#if LOGGING (LOG_LEVEL_TRACE)
  #define TRACE(message) (elvin_log (LOG_LEVEL_TRACE, message))
#else
  #define TRACE(message)
#endif

#if LOGGING (LOG_LEVEL_DIAGNOSTIC)
  #define DIAGNOSTIC(message) (elvin_log (LOG_LEVEL_DIAGNOSTIC, message)) 
  #define DIAGNOSTIC1(message, arg1) (elvin_log (LOG_LEVEL_DIAGNOSTIC, message, arg1))
  #define DIAGNOSTIC2(message, arg1, arg2) (elvin_log (LOG_LEVEL_DIAGNOSTIC, message, arg1, arg2))
#else
  #define DIAGNOSTIC(message) 
  #define DIAGNOSTIC1(message, arg1)
  #define DIAGNOSTIC2(message, arg1, arg2)
#endif

void elvin_log (int level, const char *message, ...);

#endif /*AVIS_LOG_H_*/
