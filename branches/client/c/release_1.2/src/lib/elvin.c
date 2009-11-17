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
#include <errno.h>
#include <assert.h>
#include <time.h>

#ifndef _WIN32
  #include <unistd.h>
#endif

#include <avis/stdtypes.h>
#include <avis/net.h>
#include <avis/elvin.h>
#include <avis/errors.h>
#include <avis/values.h>

#include "messages.h"
#include "listeners.h"
#include "arrays_private.h"
#include "keys_private.h"
#include "log.h"
#include "avis_client_config.h"

/**
 * ID of synthetic control messages. Using value of 0 makes
 * avis_message_free () treat it as a disposed message.
 */
#define MESSAGE_ID_CONTROL 0

/**
 * The format of a message sent via the control socket.
 */
typedef struct
{
  InvokeHandler handler;
  void *        parameter;
} ControlMessage;

static void elvin_free (Elvin *elvin);

static void elvin_shutdown (Elvin *elvin, CloseReason reason,
                            const char *message);

static bool send_liveness (Elvin *elvin);

static bool send_and_receive (Elvin *elvin, Message request,
                              Message reply, MessageTypeID reply_type);

static bool dispatch_message (Elvin *elvin, Message message);

static bool receive_reply (Elvin *elvin, Message message);

static bool poll_receive_message (Elvin *elvin, Message message);

static bool receive_control_message (socket_t input_socket, Message message,
                                     ElvinError *error);

static void handle_control_message (Elvin *elvin, Message message);

static void handle_notify_deliver (Elvin *elvin, Message message);

static void handle_nack (Message message, ElvinError *error);

static Subscription *subscription_with_id (Elvin *elvin, uint64_t id);

static void deliver_notification (Elvin *elvin, Array *ids,
                                  Attributes *attributes, bool secure);

static Subscription *elvin_subscription_init (Subscription *subscription);

static void elvin_subscription_free (Subscription *subscription);

#define str_equals(str1, str2) (strcmp ((str1), (str2)) == 0)

static bool is_supported_protocol (char **protocol);

bool elvin_open (Elvin *elvin, const char *router_uri)
{
  ElvinURI uri;

  elvin_error_init (&elvin->error);

  if (!elvin_uri_from_string (&uri, router_uri, &elvin->error))
    return false;

  elvin_open_uri (elvin, &uri);

  elvin_uri_free (&uri);

  return elvin_error_ok (&elvin->error);
}

bool elvin_open_uri (Elvin *elvin, ElvinURI *uri)
{
  return elvin_open_with_keys (elvin, uri, EMPTY_KEYS, EMPTY_KEYS);
}

bool elvin_open_with_keys (Elvin *elvin, ElvinURI *uri,
                           Keys *notification_keys, Keys *subscription_keys)
{
  alloc_message (conn_rqst);
  alloc_message (conn_rply);

  if (!is_supported_protocol (uri->protocol))
  {
    return elvin_error_set (&elvin->error, ELVIN_ERROR_USAGE,
                            "Unsupported URI protocol");
  }

  /* open router and control sockets */
  if ((elvin->router_socket =
        avis_open_socket (uri->host, uri->port, &elvin->error)) == -1)
  {
    return false;
  }

  if (!avis_open_socket_pair (&elvin->control_socket_read,
                              &elvin->control_socket_write, &elvin->error))
  {
    close_socket (elvin->router_socket);

    return false;
  }

  /* init other fields */
  elvin->polling = false;
  array_list_init (&elvin->subscriptions, sizeof (Subscription *), 5);
  listeners_init (elvin->close_listeners);
  listeners_init (elvin->notification_listeners);
  elvin->notification_keys = notification_keys;
  elvin->subscription_keys = subscription_keys;
  elvin->last_receive_time = time (NULL);
  elvin_error_init (&elvin->error);

  /* say hello to router */
  avis_message_init
    (conn_rqst, MESSAGE_ID_CONN_RQST,
     (uint32_t)uri->version_major, (uint32_t)uri->version_minor,
     EMPTY_ATTRIBUTES, notification_keys, subscription_keys);

  if (send_and_receive (elvin, conn_rqst, conn_rply, MESSAGE_ID_CONN_RPLY))
  {
    /* TODO check message reply options */

    avis_message_free (conn_rply);
  }

  return elvin_error_ok (&elvin->error);
}

bool elvin_is_open (const Elvin *elvin)
{
  return elvin->router_socket != -1;
}

bool elvin_close (Elvin *elvin)
{
  alloc_message (disconn_rqst);
  alloc_message (disconn_rply);
  bool socket_opened = elvin->router_socket != -1;

  if (socket_opened)
  {
    avis_message_init (disconn_rqst, MESSAGE_ID_DISCONN_RQST);

    send_and_receive 
      (elvin, disconn_rqst, disconn_rply, MESSAGE_ID_DISCONN_RPLY);

    elvin_shutdown (elvin, REASON_CLIENT_SHUTDOWN, "Client is closing");
  }
  
  elvin_free (elvin);
        
  return socket_opened;
}

void elvin_reset (Elvin *elvin)
{
  memset (elvin, 0, sizeof (Elvin));
  elvin->router_socket = -1;
}

void elvin_shutdown (Elvin *elvin, CloseReason reason, const char *message)
{
  ListenersIterator l;

  if (elvin->router_socket == -1)
    return;

  close_socket (elvin->router_socket);
  avis_close_socket_pair (elvin->control_socket_write,
                          elvin->control_socket_read);

  for_each_listener (elvin->close_listeners, l)
    (*l.entry->listener) (elvin, reason, message, l.entry->user_data);
}

void elvin_free (Elvin *elvin)
{
  Subscription **sub = elvin->subscriptions.items;
  size_t i;

  for (i = elvin->subscriptions.item_count; i > 0; i--, sub++)
    elvin_subscription_free (*sub);

  array_list_free (&elvin->subscriptions);

  elvin_keys_destroy (elvin->notification_keys);
  elvin_keys_destroy (elvin->subscription_keys);

  listeners_free (&elvin->close_listeners);
  listeners_free (&elvin->notification_listeners);

  elvin_error_free (&elvin->error);
  
  elvin_reset (elvin);
}

bool elvin_event_loop (Elvin *elvin)
{
  while (elvin_is_open (elvin) && elvin_error_ok (&elvin->error))
  {
    elvin_poll (elvin);

    /* timeout is OK, continue loop */
    if (elvin->error.code == ELVIN_ERROR_TIMEOUT)
      elvin_error_reset (&elvin->error);
  }

  return elvin_error_ok (&elvin->error);
}

bool elvin_poll (Elvin *elvin)
{
  time_t liveness_sent_at = 0;
  alloc_message (message);

  if (elvin->polling)
  {
    return elvin_error_set (&elvin->error, ELVIN_ERROR_USAGE,
                            "elvin_poll () called concurrently");
  }

  /* maybe send a liveness test message */
  if (time (NULL) - elvin->last_receive_time > AVIS_LIVENESS_IDLE_INTERVAL)
  {
    liveness_sent_at = time (NULL);

    if (!send_liveness (elvin))
      return false;
  }

  elvin->polling = true;

  if (poll_receive_message (elvin, message))
  {
    if (!dispatch_message (elvin, message))
    {
      elvin_error_set
        (&elvin->error, ELVIN_ERROR_PROTOCOL,
         "Unexpected message type from router: %u", 
         message_type_of (message));
    }

    avis_message_free (message);
  }

  elvin->polling = false;

  if (elvin->error.code == ELVIN_ERROR_PROTOCOL)
  {
    elvin_shutdown (elvin, REASON_PROTOCOL_VIOLATION, 
                    elvin->error.message);
  } else if (elvin->last_receive_time < liveness_sent_at)
  {
    /* 
     * if we got here on a timeout and still no router response, flip error
     * to become a liveness failure.
     */
    if (elvin->error.code == ELVIN_ERROR_TIMEOUT)
    {
      elvin_error_reset (&elvin->error);
      
      elvin_error_set (&elvin->error, ELVIN_ERROR_ROUTER_FAILURE, 
                       "Router has stopped responding");
    }
    
    elvin_shutdown (elvin, REASON_ROUTER_STOPPED_RESPONDING, 
                    elvin->error.message);
  }

  return elvin_error_ok (&elvin->error);
}

/**
 * Poll the router and control sockets for incoming messages.
 */
bool poll_receive_message (Elvin *elvin, Message message)
{
  socket_t ready_socket =
    avis_select_ready (elvin->router_socket,
                       elvin->control_socket_read, &elvin->error);

  if (ready_socket == elvin->router_socket)
  {
    elvin->last_receive_time = time (NULL);

    return avis_receive_message (elvin->router_socket, message, 
                                 &elvin->error);
  } else if (ready_socket == elvin->control_socket_read)
  {
    return receive_control_message (elvin->control_socket_read, message,
                                    &elvin->error);
  } else
  {
    return false;
  }
}

/**
 * Try to dispatch an incoming message from the router. Returns true
 * if handled, false if not. The connection may be closed on return if
 * a Disconn or DisconnRply is handled.
 */
bool dispatch_message (Elvin *elvin, Message message)
{
  bool handled = true;

  switch (message_type_of (message))
  {
  case MESSAGE_ID_CONTROL:
    handle_control_message (elvin, message);
    break;
  case MESSAGE_ID_NOTIFY_DELIVER:
    handle_notify_deliver (elvin, message);
    break;
  case MESSAGE_ID_DISCONN:
    elvin_shutdown (elvin, REASON_ROUTER_SHUTDOWN, ptr_at_offset (message, 4));
    break;
  case MESSAGE_ID_DROP_WARN:
    DIAGNOSTIC ("Router sent a dropped packet warning");
    break;
  case MESSAGE_ID_CONF_CONN:
    DIAGNOSTIC ("Router repied to a liveness check");
    break;
  default:
    handled = false;
  }

  return handled;
}

bool send_liveness (Elvin *elvin)
{
  alloc_message (test_conn);
  
  DIAGNOSTIC ("Liveness check: sending TestConn");

  avis_message_init (test_conn, MESSAGE_ID_TEST_CONN);

  return avis_send_message (elvin->router_socket, test_conn, 
                            &elvin->error);
}

bool elvin_invoke (Elvin *elvin, InvokeHandler handler, void *parameter)
{
  ControlMessage message;
  size_t bytes_written;
   
  message.handler = handler;
  message.parameter = parameter;

  bytes_written = 
    pipe_write (elvin->control_socket_write, &message, sizeof (message));
                     
  if (bytes_written == sizeof (message))
    return true;
  else
    return elvin_error_from_pipe (&elvin->error);                     
}

static void elvin_invoke_close_handler (Elvin *elvin, void *param)
{
  elvin_close (elvin);
}

bool elvin_invoke_close (Elvin *elvin)
{
  return elvin_invoke (elvin, elvin_invoke_close_handler, NULL);
}

bool receive_control_message (socket_t input_socket, Message message,
                              ElvinError *error)
{
  size_t bytes_read;

  message_type_of (message) = MESSAGE_ID_CONTROL;

  bytes_read = pipe_read (input_socket, message + 4, sizeof (ControlMessage));

  if (bytes_read == sizeof (ControlMessage))
    return true;
  else
    return elvin_error_from_pipe (error);
}

void handle_control_message (Elvin *elvin, Message message)
{
  ControlMessage *control_message = (ControlMessage *)(message + 4);

  (*control_message->handler) (elvin, control_message->parameter);
}

void handle_nack (Message nack, ElvinError *error)
{
  uint32_t error_code = int32_at_offset (nack, 4);
  const char *message = ptr_at_offset (nack, 8);

  /* TODO handle Mantara message args */
  /* Array *args = ptr_at_offset (nack, 8 + sizeof (char *)); */

  /* 21xx NACK code => subscription error */
  if (error_code / 100 == 21)
  {
    if (error_code == NACK_PARSE_ERROR)
    {
      elvin_error_set (error, ELVIN_ERROR_SYNTAX,
                       "Syntax error in subscription expression: %s", message);
    } else if (error_code == NACK_EXP_IS_TRIVIAL)
    {
      elvin_error_set (error, ELVIN_ERROR_TRIVIAL_EXPRESSION, message);
    } else
    {
      elvin_error_set (error, ELVIN_ERROR_SYNTAX,
                       "Invalid subscription expression: %s", message);
    }
  } else
  {
    elvin_error_set (error, ELVIN_ERROR_NACK,
                     "Router rejected request: %s", message);
  }
}

void handle_notify_deliver (Elvin *elvin, Message message)
{
  Attributes *attributes = ptr_at_offset (message, 0);
  Array *secure_matches =
    ptr_at_offset (message, sizeof (Attributes *));
  Array *insecure_matches =
    ptr_at_offset (message, sizeof (Attributes *) + sizeof (Array *));
  ListenersIterator l;

  deliver_notification (elvin, secure_matches, attributes, true);

  if (elvin_error_occurred (&elvin->error))
    return;

  deliver_notification (elvin, insecure_matches, attributes, false);

  /* deliver to general notification listeners */
  for_each_listener (elvin->notification_listeners, l)
  {
    (*l.entry->listener) (elvin, attributes, secure_matches->item_count > 0,
                          l.entry->user_data);
  }
}

void deliver_notification (Elvin *elvin, Array *ids,
                           Attributes *attributes, bool secure)
{
  size_t i;
  int64_t *id = ids->items;
  ListenersIterator l;

  for (i = ids->item_count; i > 0; i--, id++)
  {
    Subscription *subscription = subscription_with_id (elvin, *id);

    if (subscription == NULL)
    {
      elvin_error_set (&elvin->error, ELVIN_ERROR_PROTOCOL,
                       "Invalid subscription ID from router: %llu", *id);

      return;
    }

    for_each_listener (subscription->listeners, l)
    {
      (*l.entry->listener) (subscription, attributes, secure,
                            l.entry->user_data);
    }
  }
}

bool elvin_set_keys (Elvin *elvin,
                     Keys *notification_keys, Keys *subscription_keys)
{
  KeysDelta delta_ntfn, delta_sub;
  alloc_message (sec_rqst);
  alloc_message (sec_rply);
  
  elvin_keys_compute_delta (&delta_ntfn, 
                            elvin->notification_keys, notification_keys);
  elvin_keys_compute_delta (&delta_sub, 
                            elvin->subscription_keys, subscription_keys);
  
  avis_message_init (sec_rqst, MESSAGE_ID_SEC_RQST,
                     delta_ntfn.add, delta_ntfn.del,
                     delta_sub.add, delta_sub.del);

  if (send_and_receive (elvin, sec_rqst, sec_rply, MESSAGE_ID_SEC_RPLY))
  {
    elvin_keys_destroy (elvin->notification_keys);
    elvin_keys_destroy (elvin->subscription_keys);

    elvin->notification_keys = notification_keys;
    elvin->subscription_keys = subscription_keys;
  }
  
  elvin_keys_free_shallow (delta_ntfn.add);
  elvin_keys_free_shallow (delta_ntfn.del);
  elvin_keys_free_shallow (delta_sub.add);
  elvin_keys_free_shallow (delta_sub.del);
  
  free (delta_ntfn.add);
  free (delta_ntfn.del);
  free (delta_sub.add);
  free (delta_sub.del);
  
  return elvin_error_ok (&elvin->error);
}

bool elvin_send (Elvin *elvin, Attributes *notification)
{
  return elvin_send_with_keys (elvin, notification, EMPTY_KEYS,
                               ALLOW_INSECURE_DELIVERY);
}

bool elvin_send_with_keys (Elvin *elvin, Attributes *notification,
                           Keys *notification_keys, SecureMode security)
{
  alloc_message (notify_emit);

  avis_message_init (notify_emit, MESSAGE_ID_NOTIFY_EMIT,
                     notification, (uint32_t)security, notification_keys);

  return avis_send_message (elvin->router_socket, notify_emit, &elvin->error);
}

Subscription *elvin_subscription_init (Subscription *subscription)
{
  subscription->elvin = NULL;
  subscription->id = 0;
  subscription->security = ALLOW_INSECURE_DELIVERY;
  subscription->keys = NULL;
  listeners_init (subscription->listeners);

  return subscription;
}

void elvin_subscription_free (Subscription *subscription)
{
  free (subscription->subscription_expr);
  listeners_free (&subscription->listeners);
  elvin_keys_destroy (subscription->keys);

  memset (subscription, 0, sizeof (Subscription));
}

Subscription *elvin_subscribe (Elvin *elvin, const char *subscription_expr)
{
  return elvin_subscribe_with_keys
    (elvin, subscription_expr, EMPTY_KEYS, ALLOW_INSECURE_DELIVERY);
}

Subscription *elvin_subscribe_with_keys (Elvin *elvin,
                                         const char *subscription_expr,
                                         Keys *keys,
                                         SecureMode security)
{
  alloc_message (sub_add_rqst);
  alloc_message (sub_rply);

  avis_message_init (sub_add_rqst, MESSAGE_ID_SUB_ADD_RQST, subscription_expr,
                     (uint32_t)security, keys);

  if (send_and_receive (elvin, sub_add_rqst, sub_rply, MESSAGE_ID_SUB_RPLY))
  {
    Subscription *subscription = emalloc (sizeof (Subscription));
    Subscription **item;
    
    elvin_subscription_init (subscription);

    subscription->elvin = elvin;
    subscription->subscription_expr = estrdup (subscription_expr);
    subscription->id = int64_at_offset (sub_rply, 4);
    subscription->keys = keys;

    item = array_list_add (&elvin->subscriptions, Subscription *);
  
    *item = subscription;
    
    /* no free needed for sub_rply */

    return subscription;
  } else
  {
    return NULL;
  }
}

bool elvin_unsubscribe (Elvin *elvin, Subscription *subscription)
{
  alloc_message (sub_del_rqst);
  alloc_message (sub_rply);
  bool succeeded = false;

  Subscription **item = 
    (Subscription **)array_list_find_ptr (&elvin->subscriptions, subscription);
  
  if (item)
  {    
    avis_message_init (sub_del_rqst, MESSAGE_ID_SUB_DEL_RQST, subscription->id);
    
    succeeded =
      send_and_receive (elvin, sub_del_rqst, sub_rply, MESSAGE_ID_SUB_RPLY);
    
    /* no free needed for sub_rply */
   
    array_list_remove_item_using_ptr
      (&elvin->subscriptions, item, sizeof (Subscription *));

    elvin_subscription_free (subscription);  
  } else
  {
    elvin_error_set (&elvin->error, ELVIN_ERROR_USAGE, "Invalid subscription");
  }
  
  return succeeded;
}

bool elvin_subscription_set_expr (Subscription *subscription,
                                  const char *subscription_expr)
{
  alloc_message (sub_mod_rqst);
  alloc_message (sub_rply);

  avis_message_init (sub_mod_rqst, MESSAGE_ID_SUB_MOD_RQST, subscription->id,
                     subscription_expr, subscription->security,
                     EMPTY_KEYS, EMPTY_KEYS);

  if (send_and_receive (subscription->elvin, sub_mod_rqst, sub_rply,
                        MESSAGE_ID_SUB_RPLY))
  {
    free (subscription->subscription_expr);

    subscription->subscription_expr = estrdup (subscription_expr);

    /* no free needed for sub_rply */

    return true;
  } else
  {
    return false;
  }
}

bool elvin_subscription_set_keys (Subscription *subscription,
                                  Keys *subscription_keys,
                                  SecureMode security)
{
  alloc_message (sub_mod_rqst);
  alloc_message (sub_rply);

  /* TODO (opt) could delta keys here to potentially reduce message size */
  avis_message_init
    (sub_mod_rqst, MESSAGE_ID_SUB_MOD_RQST, subscription->id,
     "", (uint32_t)security, subscription_keys, subscription->keys);

  if (send_and_receive (subscription->elvin, sub_mod_rqst, sub_rply,
                        MESSAGE_ID_SUB_RPLY))
  {
    elvin_keys_free (subscription->keys);

    subscription->security = security;
    subscription->keys = subscription_keys;

    /* no free needed for sub_rply */

    return true;
  } else
  {
    return false;
  }
}

bool send_and_receive (Elvin *elvin, Message request,
                       Message reply, MessageTypeID reply_type)
{
  if (avis_send_message (elvin->router_socket, request, &elvin->error) &&
      receive_reply (elvin, reply))
  {
    if (message_type_of (reply) != reply_type)
    {
      if (message_type_of (reply) == MESSAGE_ID_NACK)
      {
        handle_nack (reply, &elvin->error);
      } else
      {
        elvin_error_set
          (&elvin->error, ELVIN_ERROR_PROTOCOL,
           "Unexpected reply from router: message ID %u",
           message_type_of (reply));
      }
    } else if (xid_of (request) != xid_of (reply))
    {
      elvin_error_set
        (&elvin->error, ELVIN_ERROR_PROTOCOL,
         "Mismatched transaction ID in reply from router: %u (should be %u)",
         xid_of (reply), xid_of (request));
    }

    if (elvin_error_occurred (&elvin->error))
      avis_message_free (reply);
  }

  /* close connection on protocol error/receive timeout */
  if (elvin->error.code == ELVIN_ERROR_PROTOCOL)
  {
    elvin_shutdown (elvin, REASON_PROTOCOL_VIOLATION, 
                    elvin->error.message);
  } else if (elvin->error.code == ELVIN_ERROR_TIMEOUT)
  {
    elvin_shutdown (elvin, REASON_ROUTER_STOPPED_RESPONDING, 
                    elvin->error.message);
  }

  return elvin_error_ok (&elvin->error) && elvin_is_open (elvin);
}

/**
 * Receive a reply to a request message, handling any number of
 * preceding incoming router-initiated messages.
 */
bool receive_reply (Elvin *elvin, Message message)
{
  bool replied = false;

  while (!replied &&
         elvin_error_ok (&elvin->error) && elvin_is_open (elvin) &&
         avis_receive_message (elvin->router_socket, message, &elvin->error))
  {
    if (dispatch_message (elvin, message))
      avis_message_free (message);
    else
      replied = true;
  }

  if (elvin_error_ok (&elvin->error))
  {
    return elvin_is_open (elvin);
  } else
  {
    return false;
  }
}

Subscription *subscription_with_id (Elvin *elvin, uint64_t id)
{
  Subscription **subscription = elvin->subscriptions.items;
  size_t i;

  for (i = elvin->subscriptions.item_count; i > 0; i--, subscription++)
  {
    if ((*subscription)->id == id)
      return *subscription;
  }

  return NULL;
}

void elvin_subscription_add_listener (Subscription *subscription,
                                      SubscriptionListener listener,
                                      void *user_data)
{
  listeners_add (&subscription->listeners, (Listener)listener, user_data);
}

bool elvin_subscription_remove_listener (Subscription *subscription,
                                         SubscriptionListener listener)
{
  return listeners_remove (&subscription->listeners, (Listener)listener);
}

void elvin_add_close_listener (Elvin *elvin, CloseListener listener,
                               void *user_data)
{
  listeners_add (&elvin->close_listeners, listener, user_data);
}

bool elvin_remove_close_listener (Elvin *elvin, CloseListener listener)
{
  return listeners_remove (&elvin->close_listeners, listener);
}

void elvin_add_notification_listener (Elvin *elvin,
                                      GeneralNotificationListener listener,
                                      void *user_data)
{
  listeners_add (&elvin->notification_listeners, (Listener)listener, user_data);
}

bool elvin_remove_notification_listener (Elvin *elvin,
                                         GeneralNotificationListener listener)
{
  return listeners_remove (&elvin->notification_listeners, (Listener)listener);
}

bool is_supported_protocol (char **protocol)
{
  return protocol == DEFAULT_URI_PROTOCOL ||
         (str_equals (protocol [0], "tcp") &&
          str_equals (protocol [1], "none") &&
          str_equals (protocol [2], "xdr"));
}
