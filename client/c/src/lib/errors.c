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
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <errno.h>
#include <string.h>

#include <avis/stdtypes.h>
#include <avis/errors.h>

#include "errors_private.h"
#include "log.h"

#ifdef _WIN32
  #define vsnprintf _vsnprintf
#endif

static void try_vnsprintf (char **message, int *message_length,
                           const char *format, va_list args);

/** Generate a formatted message, allowing for expanding space */
#define avis_vnsprintf(message, format, arg) \
  { \
    va_list args; \
    int message_length = 300; \
    \
    while ((message) == NULL) \
    { \
      va_start (args, arg); \
      \
      try_vnsprintf (&(message), &message_length, format, args); \
      \
      va_end (args); \
    } \
  }

void elvin_error_init (ElvinError *error)
{
  error->code = ELVIN_ERROR_NONE;
  error->message = NULL;
}

void elvin_error_free (ElvinError *error)
{
  if (error->message)
  {
    free (error->message);

    error->message = NULL;
  }

  error->code = ELVIN_ERROR_NONE;
}

void elvin_perror (const char *tag, ElvinError *error)
{
  if (error->message == NULL)
    fprintf (stderr, "elvin_perror () called with no message\n");
  else
    fprintf (stderr, "%s: %s\n", tag, error->message);
}

bool elvin_error_set (ElvinError *error, int code, const char *message, ...)
{
  /* do not allow overrride of earlier error */
  if (error->code != ELVIN_ERROR_NONE)
  {
    DIAGNOSTIC2 
      ("Ignoring error due to existing error state: ignored error \"%s\" (%u)", 
        message, code);

    return false;
  }

  error->code = code;
  error->message = NULL;

  avis_vnsprintf (error->message, message, message);

  return false;
}

bool elvin_error_from_errno (ElvinError *error)
{
  error->code = errno_to_elvin_error (errno);
  error->message = estrdup (strerror (errno));

  return false;
}

void *do_avis_emalloc (size_t size, const char *file, int line)
{
  void *data = malloc (size);

  if (!data)
    avis_fail ("Out of memory: could not allocate %ul bytes", file, line, size);

   return data;
}

char *do_avis_estrdup (const char *str, const char *file, int line)
{
  char *str_copy = strdup (str);

  if (str_copy == NULL)
    avis_fail ("Out of memory: could not copy string", file, line);

  return str_copy;
}

static bool in_fail = false;

void avis_fail (const char *message, const char *file, int line, ...)
{
  char *formatted_message = NULL;

  if (in_fail)
  {
    fprintf (stderr, "Avis client library multiple failures\n");

    exit (1);
  }

  in_fail = true;

  avis_vnsprintf (formatted_message, message, line);

  fprintf
    (stderr,
     "Avis client library fatal error: file %s, line %i: %s\n", file, line,
     formatted_message);

  free (formatted_message);

  exit (1);
}

/**
 * Try to vnsprintf a message.
 */
void try_vnsprintf (char **message, int *message_length, const char *format,
                     va_list args)
{
  int chars_written;

  /* generate a formatted message, allowing for expanding space */
  if (*message == NULL)
  {
    *message = emalloc (*message_length);
  } else
  {
    *message = realloc (*message, *message_length);

    if (*message == NULL)
      error_fail ("Failed to allocate error message");
  }

  /* Try to print in the allocated space. */

  chars_written = vsnprintf (*message, *message_length, format, args);

  /* If that worked, done */
  if (chars_written > -1 && chars_written < *message_length)
    return;

  /* Else try again with more space. */
  if (chars_written > -1) /* glibc 2.1 */
    *message_length = chars_written + 1; /* precisely what is needed */
  else /* glibc 2.0 */
    *message_length *= 2; /* twice the old message_length */
}
