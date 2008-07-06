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

  /* basic URI */
  elvin_uri_from_string (&uri, "elvin://host", &error);
  fail_on_error (&error);

  fail_unless (strcmp ("host", uri.host) == 0, "Bad host: %s", uri.host);
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
}
END_TEST


START_TEST (test_version)
{
  ElvinURI uri;

  elvin_uri_from_string
    (&uri, "elvin:5.1/xdr,none,ssl/host:4567?name1=value1;name2=value2",
     &error);
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
}
END_TEST

START_TEST (test_ipv6)
{
  ElvinURI uri;

  /* IPv6 */
  elvin_uri_from_string (&uri, "elvin://[::1]", &error);
  fail_on_error (&error);

  fail_unless
    (strcmp (uri.host, "::1") == 0, "Bad IPv6 host: %s", uri.host);

  elvin_uri_from_string
    (&uri, "elvin://[2001:0db8:85a3:08d3:1319:8a2e:0370:7344]:1234", &error);
  fail_on_error (&error);

  fail_unless
    (strcmp (uri.host, "2001:0db8:85a3:08d3:1319:8a2e:0370:7344") == 0,
     "Bad IPv6 host: %s", uri.host);
  fail_unless (uri.port == 1234, "Bad port: %i", uri.port);
}
END_TEST

START_TEST (test_invalid)
{
  ElvinURI uri;

  /* invaid URI's */
  check_invalid_uri ("hello://host");
  check_invalid_uri ("elvin://host:1234567890");
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
  tcase_add_test (tc_core, test_invalid);

  tcase_add_checked_fixture (tc_core, setup, teardown);

  return tc_core;
}
