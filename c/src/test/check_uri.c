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
#include <string.h>

#include <check.h>

#include "avis/elvin.h"
#include "avis/errors.h"
#include "errors_private.h"

#include "check_ext.h"

static ElvinError error = ELVIN_EMPTY_ERROR;

static void setup ()
{
}

static void teardown ()
{
  elvin_error_free (&error);
}

#define check_invalid_uri(uri_string)\
{\
  elvin_uri_from_string (&uri, uri_string, &error);\
  fail_unless_error_code (&error, ELVIN_ERROR_INVALID_URI);\
  reset_uri (uri);\
}

#define reset_uri(uri) \
  (elvin_uri_free (&uri), uri.host = NULL, uri.port = 0, \
   uri.version_major = uri.version_minor = 0)

START_TEST (test_basic)
{
  ElvinURI uri;

  elvin_uri_from_string (&uri, "elvin://host", &error);
  fail_on_error (&error);

  fail_unless (strcmp ("host", uri.host) == 0, "Bad host: %s", uri.host);

  elvin_uri_free (&uri);
}
END_TEST

START_TEST (test_host_and_port)
{
  ElvinURI uri;

  elvin_uri_from_string (&uri, "elvin://host:1234", &error);
  fail_on_error (&error);

  fail_unless (strcmp ("host", uri.host) == 0, "Bad host: %s", uri.host);
  fail_unless (uri.port == 1234, "Bad port: %u", uri.port);
  fail_unless (uri.version_major == DEFAULT_CLIENT_PROTOCOL_MAJOR,
               "Bad major version: %u", uri.version_major);
  fail_unless (uri.version_minor == DEFAULT_CLIENT_PROTOCOL_MINOR,
               "Bad minor version: %u", uri.version_minor);

  elvin_uri_free (&uri);
}
END_TEST


START_TEST (test_version)
{
  ElvinURI uri;

  elvin_uri_from_string (&uri, "elvin:5.1//host:4567",&error);
  fail_on_error (&error);

  fail_unless (strcmp ("host", uri.host) == 0, "Bad host: %s", uri.host);
  fail_unless (uri.port == 4567, "Bad port: %u", uri.port);
  fail_unless (uri.version_major == 5, "Bad major version: %u", uri.version_major);
  fail_unless (uri.version_minor == 1, "Bad minor version: %u", uri.version_minor);

  reset_uri (uri);
  elvin_uri_from_string (&uri, "elvin:5//host", &error);
  fail_on_error (&error);

  fail_unless (uri.version_major == 5,
               "Bad major version: %u", uri.version_major);
  fail_unless (uri.version_minor == 0,
               "Bad minor version: %u", uri.version_minor);

  elvin_uri_free (&uri);
}
END_TEST

START_TEST (test_protocol)
{
  ElvinURI uri;

  elvin_uri_from_string (&uri, "elvin:/foo,bar,baz/host", &error);
  fail_on_error (&error);

  fail_unless (strcmp (uri.protocol [0], "foo") == 0, "Bad protocol: %s",
               uri.protocol [0]);
  fail_unless (strcmp (uri.protocol [1], "bar") == 0, "Bad protocol: %s",
               uri.protocol [1]);
  fail_unless (strcmp (uri.protocol [2], "baz") == 0, "Bad protocol: %s",
               uri.protocol [2]);

  reset_uri (uri);

  /* secure */
  elvin_uri_from_string (&uri, "elvin:/secure/host", &error);
  fail_on_error (&error);

  fail_unless (strcmp (uri.protocol [0], "xdr") == 0, "Bad protocol: %s",
               uri.protocol [0]);
  fail_unless (strcmp (uri.protocol [1], "none") == 0, "Bad protocol: %s",
               uri.protocol [1]);
  fail_unless (strcmp (uri.protocol [2], "ssl") == 0, "Bad protocol: %s",
               uri.protocol [2]);

  reset_uri (uri);

  check_invalid_uri ("elvin:/tcp/host");
  check_invalid_uri ("elvin:/tcp,none/host");
}
END_TEST

START_TEST (test_ipv6)
{
  ElvinURI uri;

  elvin_uri_from_string (&uri, "elvin://[::1]", &error);
  fail_on_error (&error);

  fail_unless
    (strcmp (uri.host, "::1") == 0, "Bad IPv6 host: %s", uri.host);

  elvin_uri_free (&uri);

  elvin_uri_from_string
    (&uri, "elvin://[2001:0db8:85a3:08d3:1319:8a2e:0370:7344]:1234", &error);
  fail_on_error (&error);

  fail_unless
    (strcmp (uri.host, "2001:0db8:85a3:08d3:1319:8a2e:0370:7344") == 0,
     "Bad IPv6 host: %s", uri.host);
  fail_unless (uri.port == 1234, "Bad port: %i", uri.port);

  reset_uri (uri);

  check_invalid_uri ("elvin://[");
  check_invalid_uri ("elvin://[[");
  check_invalid_uri ("elvin://[]]");
}
END_TEST

static const char *base_long_uri = "elvin://host?name=";
#define base_long_uri_length 18

START_TEST (test_options)
{
  ElvinURI uri;
  char long_option_uri [500 + base_long_uri_length + 1];

  memset (long_option_uri, 'x', sizeof (long_option_uri));
  memcpy (long_option_uri, base_long_uri, base_long_uri_length);
  long_option_uri [sizeof (long_option_uri) - 1] = '\0';

  elvin_uri_from_string (&uri, "elvin://host?name1=value1", &error);
  fail_on_error (&error);

  fail_unless
    (attributes_size (uri.options) == 1, "Wrong number of attributes");

  fail_unless
    (strcmp (attributes_get_string (uri.options, "name1"), "value1") == 0,
     "Value wrong: %s != value1", attributes_get_string (uri.options, "name1"));

  reset_uri (uri);

  elvin_uri_from_string (&uri, "elvin://host?name1=value1;name2=value2", &error);
  fail_on_error (&error);

  fail_unless
    (attributes_size (uri.options) == 2, "Wrong number of attributes");

  fail_unless
    (strcmp (attributes_get_string (uri.options, "name1"), "value1") == 0,
     "Value wrong: %s != value1", attributes_get_string (uri.options, "name1"));

  fail_unless
    (strcmp (attributes_get_string (uri.options, "name2"), "value2") == 0,
     "Value wrong: %s != value2", attributes_get_string (uri.options, "name2"));

  /* check escapes */
  reset_uri (uri);

  elvin_uri_from_string (&uri, "elvin://host?name\\=1=value1;name2=value2", &error);
  fail_on_error (&error);

  fail_unless
    (attributes_size (uri.options) == 2, "Wrong number of attributes");

  fail_unless
    (strcmp (attributes_get_string (uri.options, "name=1"), "value1") == 0,
     "Value wrong: %s != value1", attributes_get_string (uri.options, "name=1"));

  reset_uri (uri);

  check_invalid_uri ("elvin://host?");
  check_invalid_uri ("elvin://host?x");
  check_invalid_uri ("elvin://host?=");
  check_invalid_uri ("elvin://host?name1=");
  check_invalid_uri ("elvin://host?=value");
  check_invalid_uri ("elvin://host?name==value");
  check_invalid_uri ("elvin://host?name=value;x");
  check_invalid_uri ("elvin://host?name=\\");
  check_invalid_uri (long_option_uri);
}
END_TEST

START_TEST (test_invalid)
{
  ElvinURI uri;

  check_invalid_uri ("hello://host");
  check_invalid_uri ("elvin://host:1234567890");
  check_invalid_uri ("elvin://host:1234:1234");
  check_invalid_uri ("elvin://host:-1");
  check_invalid_uri ("elvin://host:hello");
  check_invalid_uri ("elvin://:1234");
  check_invalid_uri ("elvin:hello//host");
  check_invalid_uri ("elvin:4.//host");
  check_invalid_uri ("elvin:-4//host");
  check_invalid_uri ("elvin:4e//host");
  check_invalid_uri ("elvin://");
  check_invalid_uri ("elvin:/");
  check_invalid_uri ("elvin::/");
  check_invalid_uri ("elvin:");
  check_invalid_uri ("elvin");
  check_invalid_uri ("");
}
END_TEST

TCase *uri_tests ()
{
  TCase *tc_core = tcase_create ("uri");
  tcase_add_test (tc_core, test_basic);
  tcase_add_test (tc_core, test_host_and_port);
  tcase_add_test (tc_core, test_version);
  tcase_add_test (tc_core, test_ipv6);
  tcase_add_test (tc_core, test_protocol);
  tcase_add_test (tc_core, test_options);
  tcase_add_test (tc_core, test_invalid);

  tcase_add_checked_fixture (tc_core, setup, teardown);

  return tc_core;
}
