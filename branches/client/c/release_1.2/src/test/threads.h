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
#ifndef AVIS_THREADS_H_
#define AVIS_THREADS_H_

#ifdef WIN32
  typedef HANDLE thread_t;

  #define create_thread(thread, handler, param) \
    ((thread = CreateThread (NULL, 0, handler, param, 0, NULL)) == NULL)

  #define sleep(secs) Sleep (secs * 1000)

  #define decl_thread_proc(name, param) DWORD WINAPI name (LPVOID param)
#else
  #include <unistd.h>
  #include <pthread.h>

  typedef pthread_t thread_t;

  #define create_thread(thread, handler, param) \
    pthread_create (&thread, NULL, handler, param)

  #define decl_thread_proc(name, param) void *name (void *param)
#endif

#endif /* AVIS_THREADS_H_ */
