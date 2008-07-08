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
#include <string.h>

#include <avis/elvin.h>
#include <avis/elvin_uri.h>

#include "errors_private.h"

static bool parse_version (ElvinURI *uri,
                           const char *index,
                           ElvinError *error);

static char *substring (const char *start, const char *end);

static const char *find_chars (const char *start, const char *chars);

static const char *find_char (const char *start, char c);

static const char *parse_options (ElvinURI *uri, const char *index,
                                  ElvinError *error);

#define fail_if(expr,message) \
  if (expr) \
    return elvin_error_set (error, ELVIN_ERROR_INVALID_URI, message);

#define fail(message) \
  elvin_error_set (error, ELVIN_ERROR_INVALID_URI, message);

void elvin_uri_free (ElvinURI *uri)
{
  if (uri->host)
  {
    free (uri->host);

    uri->host = NULL;
  }
}

bool elvin_uri_from_string (ElvinURI *uri, const char *uri_string,
                            ElvinError *error)
{
  const char *index1 = uri_string;
  const char *index2;
  long port;

  uri->host = NULL;
  uri->port = DEFAULT_ELVIN_PORT;
  uri->version_major = DEFAULT_CLIENT_PROTOCOL_MAJOR;
  uri->version_minor = DEFAULT_CLIENT_PROTOCOL_MINOR;
  uri->options = EMPTY_ATTRIBUTES;

  index2 = strchr (index1, ':');

  fail_if (index2 == NULL, "No URI scheme present");
  fail_if (memcmp ("elvin", index1, index2 - index1) != 0, "Not an Elvin URI");

  /* first slash */
  index2++;
  index1 = strchr (index2, '/');
  fail_if (index1 == NULL, "No host name present");

  if (index1 != index2)
  {
    /* parse version: elvin:<major>.<minor>//... */
    if (!parse_version (uri, index2, error))
      return false;
  }

  /* second slash */
  index2 = strchr (index1 + 1, '/');
  fail_if (index2 == NULL, "Missing second /");

  if (index2 != index1 + 1)
  {
    /* TODO parse protocol stack */
  }

  index1 = index2 + 1;

  fail_if (*index1 == '\0', "Missing hostname");

  if (*index1 == '[')
  {
    index1++;

    /* IPv6 addess */
    index2 = find_char (index1, ']');

    fail_if (*index2 == '\0', "Missing closing ']' in IPv6 address");

    uri->host = substring (index1, index2);

    index1 = index2 + 1;
  } else
  {
    index2 = find_chars (index1, ":?");

    fail_if (index1 == index2, "Missing hostname");

    uri->host = substring (index1, index2);

    index1 = index2;
  }

  if (*index1 == ':')
  {
    index1++;

    port = strtol (index1, (char **)&index2, 10);

    fail_if (index1 == index2 || port < 0 || port > 65535,
             "Invalid port number");

    uri->port = (uint16_t)port;

    index1 = index2;
  }

  if (*index1 == '?')
  {
    /* parse options: elvin://...?name1=value1;name2=value2 */
    if ((index1 = parse_options (uri, index1 + 1, error)) == NULL)
      return false;
  }

  fail_if (*index1 != '\0', "Junk at end of URI");

  return true;
}

bool parse_version (ElvinURI *uri, const char *index, ElvinError *error)
{
  char *index2;
  long value = strtol (index, &index2, 10);

  fail_if (index == index2 || value < 0, "Invalid version number");

  uri->version_major = (uint16_t)value;

  if (*index2 == '.')
  {
    index = index2 + 1;

    value = strtol (index, &index2, 10);

    fail_if (index == index2 || value < 0, "Invalid version number");

    uri->version_minor = (uint16_t)value;
  } else
  {
    uri->version_minor = 0;
  }

  fail_if (*index2 != '/', "Junk at end of version number");

  return true;
}

/** Max length of an option name or value */
#define MAX_OPTION_LENGTH 255

/** Finish adding chars to the buffer: generate a string and reset */
#define buffer_finish(buff, curr) (*curr = '\0', curr = buff, strdup (buff))

const char *parse_options (ElvinURI *uri, const char *index,
                           ElvinError *error)
{
  Attributes *options = attributes_create ();
  char buffer [MAX_OPTION_LENGTH + 1];
  char *name = NULL;
  char *current = buffer;
  char *end = buffer + MAX_OPTION_LENGTH;

  while (true)
  {
    switch (*index)
    {
    case ';':
    case '\0':
      if (name != NULL && current != buffer)
      {
        attributes_set_string (options, name, buffer_finish (buffer, current));
        name = NULL;
      } else
      {
        fail ("Missing option value");
      }
      break;
    case '=':
      if (name == NULL)
      {
        if (current != buffer)
          name = buffer_finish (buffer, current);
        else
          fail ("Missing option value");
      } else
      {
        fail ("Missing option name");
      }
      break;
    case '\\':
      index++;

      if (*index == '\0')
      {
        fail ("Trailing \\");
        break;
      }
      /* drop through to default append behaviour */
    default:
      if (current < end)
        *current++ = *index;
      else
        fail (name ? "Name too long" : "Value too long");
    }

    if (*index == '\0')
      break;
    else
      index++;
  }

  if (name != NULL)
    free (name);

  if (elvin_error_ok (error))
  {
    uri->options = options;

    return index;
  } else
  {
    attributes_free (options);

    return NULL;
  }
}

/**
 * Find the first occurrence of any character in chars, returning the pointer
 * to the end of the string if not.
 */
const char *find_chars (const char *start, const char *chars)
{
  const char *c;

  for ( ; *start; start++)
  {
    for (c = chars; *c; c++)
    {
      if (*c == *start)
        return start;
    }
  }

  return start;
}

const char *find_char (const char *start, char c)
{
  for ( ; *start && *start != c; start++) { /* zip */ }

  return start;
}

/**
 * Create a substring between start (inclusive) and end (exclusive).
 */
char *substring (const char *start, const char *end)
{
  size_t length = end - start;
  char *new_str = emalloc (length + 1);

  memcpy (new_str, start, length);

  new_str [length] = '\0';

  return new_str;
}
