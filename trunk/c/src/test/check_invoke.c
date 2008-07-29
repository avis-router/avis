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

#include <avis/elvin.h>
#include <avis/errors.h>
#include <avis/stdtypes.h>
#include "errors_private.h"
#include "avis/attributes.h"

#include "threads.h"
#include "messages.h"
#include "byte_buffer.h"
#include "check_ext.h"

const char *elvin_router ()
{
  const char *uri = getenv ("ELVIN");

  return uri == NULL ? "elvin://localhost" : uri;
}

/**
 * Send a notification.
 */
static void notify_thread_send (Elvin *elvin, void *param)
{
  Attributes *ntfn = attributes_create ();

  attributes_set_int32 (ntfn, "test", 1);

  elvin_send (elvin, ntfn);

  attributes_destroy (ntfn);
}

/**
 * Trigger notify_thread_send ().
 */
decl_thread_proc (notify_thread_main, elvin)
{
  elvin_invoke (elvin, notify_thread_send, NULL);

  return NULL;
}

/**
 * Handle notification by closing connection.
 */
static void notify_listener (Subscription *subscription,
                            Attributes *attributes, bool secure,
                            void *user_data)
{
  elvin_close (subscription->elvin);
}

/**
 * Test using elvin_invoke () to send a notification that triggers a call
 * to elvin_close ();
 */
START_TEST (test_notify)
{
  Elvin elvin;
  thread_t notify_thread;
  Subscription *sub;

  elvin_open (&elvin, elvin_router ());
  fail_on_error (&elvin.error);

  sub = elvin_subscribe (&elvin, "test == 1");
  fail_on_error (&elvin.error);

  elvin_subscription_add_listener (sub, notify_listener, NULL);

  fail_if (create_thread (notify_thread, notify_thread_main, &elvin),
           "Thread create failed");

  /* notification listener should trigger close: test will timeout if not */
  elvin_event_loop (&elvin);
  fail_on_error (&elvin.error);

  elvin_close (&elvin);
}
END_TEST

TCase *invoke_tests ()
{
  TCase *tc_core = tcase_create ("invoke");

  tcase_add_test (tc_core, test_notify);

  return tc_core;
}
