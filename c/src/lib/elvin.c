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

#ifdef WIN32
  #include <winsock2.h>
  #include <ws2tcpip.h>

  #define close_socket(s) closesocket (s)
  #define snprintf _snprintf
#else
  #include <unistd.h>
  #include <sys/types.h>
  #include <sys/socket.h>
  #include <netdb.h>
  #include <netinet/in.h>
  #include <arpa/inet.h>

  #define close_socket(s) close (s)
#endif

#include <avis/stdtypes.h>
#include <avis/elvin.h>
#include <avis/errors.h>
#include <avis/values.h>

#include "messages.h"
#include "listeners.h"
#include "arrays_private.h"
#include "log.h"
#include "avis_client_config.h"

static void elvin_shutdown (Elvin *elvin, CloseReason reason,
                            const char *message);

static bool open_socket (Elvin *elvin, const char *host, uint16_t port,
                         ElvinError *error);

static bool send_and_receive (Elvin *elvin, Message request,
                              Message reply, MessageTypeID reply_type,
                              ElvinError *error);

static bool send_message (Elvin *elvin, Message message, ElvinError *error);

static bool receive_message (Elvin *elvin, Message message, ElvinError *error);

static void handle_notify_deliver (Elvin *elvin, Message message,
                                   ElvinError *error);

static void handle_nack (Elvin *elvin, Message message, ElvinError *error);

static Subscription *subscription_with_id (Elvin *elvin, uint64_t id);

static void deliver_notification (Elvin *elvin, Array *ids,
                                  Attributes *attributes, bool secure,
                                  ElvinError *error);

static Subscription *elvin_subscription_init (Subscription *subscription);

static void elvin_subscription_free (Subscription *subscription);

#ifdef WIN32
  static void init_windows_sockets (ElvinError *error);
#endif

bool elvin_open (Elvin *elvin, const char *router_uri, ElvinError *error)
{
  ElvinURI uri;

  if (!elvin_uri_from_string (&uri, router_uri, error))
    return false;

  elvin_open_uri (elvin, &uri, error);

  elvin_uri_free (&uri);

  return elvin_error_ok (error);
}

bool elvin_open_uri (Elvin *elvin, ElvinURI *uri, ElvinError *error)
{
  return elvin_open_with_keys (elvin, uri, EMPTY_KEYS, EMPTY_KEYS, error);
}

bool elvin_open_with_keys (Elvin *elvin, ElvinURI *uri,
                           Keys *notification_keys, Keys *subscription_keys,
                           ElvinError *error)
{
  alloc_message (conn_rqst);
  alloc_message (conn_rply);

  elvin->socket = -1;
  elvin->polling = false;
  array_list_init (&elvin->subscriptions, sizeof (Subscription), 5);
  listeners_init (elvin->close_listeners);
  listeners_init (elvin->notification_listeners);
  elvin->notification_keys = notification_keys;
  elvin->subscription_keys = subscription_keys;

  if (!open_socket (elvin, uri->host, uri->port, error))
    return false;

  message_init (conn_rqst, MESSAGE_ID_CONN_RQST,
                (uint32_t)uri->version_major, (uint32_t)uri->version_minor,
                EMPTY_ATTRIBUTES, notification_keys, subscription_keys);

  if (send_and_receive (elvin, conn_rqst, conn_rply,
                        MESSAGE_ID_CONN_RPLY, error))
  {

    /* TODO check message reply options */

    message_free (conn_rply);
  }

  return elvin_error_ok (error);
}

bool elvin_is_open (Elvin *elvin)
{
  return elvin->socket != -1;
}

#ifdef WIN32
  #include <windows.h>
  #define sleep_for(t) Sleep (t)
#else
  #define sleep_for(t) usleep (t)
#endif

bool elvin_close (Elvin *elvin)
{
  ElvinError error = ELVIN_EMPTY_ERROR;
  alloc_message (disconn_rqst);
  alloc_message (disconn_rply);

  if (elvin->socket == -1)
    return false;

  message_init (disconn_rqst, MESSAGE_ID_DISCONN_RQST);

  if (elvin->polling)
  {
    if (send_message (elvin, disconn_rqst, &error))
    {
      time_t start_time = time (NULL);

      /* wait for poll loop to shutdown connection */
      while (elvin->socket != -1 &&
             difftime (time (NULL), start_time) < AVIS_IO_TIMEOUT);
      {
        sleep_for (200);
      }
    }
  } else
  {
    send_and_receive (elvin, disconn_rqst, disconn_rply,
                      MESSAGE_ID_DISCONN_RPLY, &error);

  }

  elvin_shutdown (elvin, REASON_CLIENT_SHUTDOWN, "Client is closing");

  elvin_error_free (&error);

  return true;
}

void elvin_shutdown (Elvin *elvin, CloseReason reason, const char *message)
{
  size_t i;
  Subscription *sub = elvin->subscriptions.items;
  ListenersIterator l;

  if (elvin->socket == -1)
    return;

  close_socket (elvin->socket);

  elvin->socket = -1;

  for (i = elvin->subscriptions.item_count; i > 0; i--, sub++)
    elvin_subscription_free (sub);

  array_list_free (&elvin->subscriptions);

  elvin_keys_destroy (elvin->notification_keys);
  elvin_keys_destroy (elvin->subscription_keys);

  for_each_listener (elvin->close_listeners, l)
    (*l.entry->listener) (elvin, reason, message, l.entry->user_data);

  listeners_free (&elvin->close_listeners);
  listeners_free (&elvin->notification_listeners);

  #ifdef WIN32
    WSACleanup ();
  #endif
}

bool elvin_event_loop (Elvin *elvin, ElvinError *error)
{
  while (elvin->socket != -1 && elvin_error_ok (error))
  {
    elvin_poll (elvin, error);

    /* just keep looping on timeout */
    if (error->code == ELVIN_ERROR_TIMEOUT)
      elvin_error_reset (error);
  }

  return elvin_error_ok (error);
}

bool elvin_poll (Elvin *elvin, ElvinError *error)
{
  alloc_message (message);

  if (elvin->polling)
  {
    elvin_error_set (error, ELVIN_ERROR_USAGE, "poll () called concurrently");

    return false;
  }

  elvin->polling = true;

  if (receive_message (elvin, message, error))
  {
    switch (message_type_of (message))
    {
    case MESSAGE_ID_NOTIFY_DELIVER:
      handle_notify_deliver (elvin, message, error);
      break;
    case MESSAGE_ID_DISCONN:
      elvin_shutdown (elvin, REASON_ROUTER_SHUTDOWN, "Router is shutting down");
      break;
    case MESSAGE_ID_DISCONN_RPLY:
      elvin_shutdown (elvin, REASON_CLIENT_SHUTDOWN, "Client is closing");
      break;
    default:
      elvin_error_set
        (error, ELVIN_ERROR_PROTOCOL,
         "Unexpected message type from router: %u", message_type_of (message));
    }

    message_free (message);
  }

  if (error->code == ELVIN_ERROR_PROTOCOL)
    elvin_shutdown (elvin, REASON_PROTOCOL_VIOLATION, error->message);

  elvin->polling = false;

  return elvin_error_ok (error);
}

void handle_nack (Elvin *elvin, Message nack, ElvinError *error)
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

void handle_notify_deliver (Elvin *elvin, Message message, ElvinError *error)
{
  Attributes *attributes = ptr_at_offset (message, 0);
  Array *secure_matches =
    ptr_at_offset (message, sizeof (Attributes *));
  Array *insecure_matches =
    ptr_at_offset (message, sizeof (Attributes *) + sizeof (Array *));
  ListenersIterator l;

  deliver_notification (elvin, secure_matches, attributes, true, error);

  if (elvin_error_occurred (error))
    return;

  deliver_notification (elvin, insecure_matches, attributes, false, error);

  /* deliver to general notification listeners */
  for_each_listener (elvin->notification_listeners, l)
  {
    (*l.entry->listener) (elvin, attributes, secure_matches->item_count > 0,
                          l.entry->user_data);
  }
}

void deliver_notification (Elvin *elvin, Array *ids,
                           Attributes *attributes, bool secure,
                           ElvinError *error)
{
  int i;
  int64_t *id = ids->items;
  ListenersIterator l;

  for (i = ids->item_count; i > 0; i--, id++)
  {
    Subscription *subscription = subscription_with_id (elvin, *id);

    if (subscription == NULL)
    {
      elvin_error_set (error, ELVIN_ERROR_PROTOCOL,
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
                     Keys *notification_keys, Keys *subscription_keys,
                     ElvinError *error)
{
  alloc_message (sec_rqst);
  alloc_message (sec_rply);

  /* TODO (opt) could compute delta here to possibly reduce message size */
  message_init (sec_rqst, MESSAGE_ID_SEC_RQST,
                notification_keys, elvin->notification_keys,
                subscription_keys, elvin->subscription_keys);

  if (send_and_receive (elvin, sec_rqst, sec_rply, MESSAGE_ID_SEC_RPLY, error))
  {
    elvin_keys_free (elvin->notification_keys);
    elvin_keys_free (elvin->subscription_keys);

    elvin->notification_keys = notification_keys;
    elvin->subscription_keys = subscription_keys;

    /* sec_rply does not need to be freed */
    return true;
  } else
  {
    return false;
  }
}

bool elvin_send (Elvin *elvin, Attributes *notification, ElvinError *error)
{
  return elvin_send_with_keys (elvin, notification, EMPTY_KEYS,
                               ALLOW_INSECURE_DELIVERY, error);
}

bool elvin_send_with_keys (Elvin *elvin, Attributes *notification,
                           Keys *notification_keys, SecureMode security,
                           ElvinError *error)
{
  alloc_message (notify_emit);

  message_init (notify_emit, MESSAGE_ID_NOTIFY_EMIT,
                notification, (uint32_t)security, notification_keys);

  return send_message (elvin, notify_emit, error);
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

Subscription *elvin_subscribe (Elvin *elvin, const char *subscription_expr,
                               ElvinError *error)
{
  return elvin_subscribe_with_keys
    (elvin, subscription_expr, EMPTY_KEYS, ALLOW_INSECURE_DELIVERY, error);
}

Subscription *elvin_subscribe_with_keys (Elvin *elvin,
                                         const char *subscription_expr,
                                         Keys *keys,
                                         SecureMode security,
                                         ElvinError *error)
{
  alloc_message (sub_add_rqst);
  alloc_message (sub_rply);

  message_init (sub_add_rqst, MESSAGE_ID_SUB_ADD_RQST, subscription_expr,
                (uint32_t)security, keys);

  if (send_and_receive (elvin, sub_add_rqst, sub_rply,
                        MESSAGE_ID_SUB_RPLY, error))
  {
    Subscription *subscription =
      array_list_add (&elvin->subscriptions, Subscription);

    elvin_subscription_init (subscription);

    subscription->elvin = elvin;
    subscription->subscription_expr = strdup (subscription_expr);
    subscription->id = int64_at_offset (sub_rply, 4);
    subscription->keys = keys;

    /* no free needed for sub_rply */

    return subscription;
  } else
  {
    return NULL;
  }
}

bool elvin_unsubscribe (Elvin *elvin, Subscription *subscription,
                        ElvinError *error)
{
  alloc_message (sub_del_rqst);
  alloc_message (sub_rply);
  bool succeeded;

  message_init (sub_del_rqst, MESSAGE_ID_SUB_DEL_RQST, subscription->id);

  succeeded =
    send_and_receive (elvin, sub_del_rqst, sub_rply,
                      MESSAGE_ID_SUB_RPLY, error);

  /* no free needed for sub_rply */

  elvin_subscription_free (subscription);

  array_list_remove_item_using_ptr
    (&elvin->subscriptions, subscription, sizeof (Subscription));

  return succeeded;
}

bool elvin_subscription_set_expr (Subscription *subscription,
                                  const char *subscription_expr,
                                  ElvinError *error)
{
  alloc_message (sub_mod_rqst);
  alloc_message (sub_rply);

  message_init (sub_mod_rqst, MESSAGE_ID_SUB_MOD_RQST, subscription->id,
                subscription_expr, subscription->security,
                EMPTY_KEYS, EMPTY_KEYS);

  if (send_and_receive (subscription->elvin, sub_mod_rqst, sub_rply,
                        MESSAGE_ID_SUB_RPLY, error))
  {
    free (subscription->subscription_expr);

    subscription->subscription_expr = strdup (subscription_expr);

    /* no free needed for sub_rply */

    return true;
  } else
  {
    return false;
  }
}

bool elvin_subscription_set_keys (Subscription *subscription,
                                  Keys *subscription_keys,
                                  SecureMode security,
                                  ElvinError *error)
{
  alloc_message (sub_mod_rqst);
  alloc_message (sub_rply);

  /* TODO (opt) could delta keys here to potentially reduce message size */
  message_init (sub_mod_rqst, MESSAGE_ID_SUB_MOD_RQST, subscription->id,
                "", (uint32_t)security, subscription_keys, subscription->keys);

  if (send_and_receive (subscription->elvin, sub_mod_rqst, sub_rply,
                        MESSAGE_ID_SUB_RPLY, error))
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
                       Message reply, MessageTypeID reply_type,
                       ElvinError *error)
{
  if (send_message (elvin, request, error) &&
      receive_message (elvin, reply, error))
  {
    if (message_type_of (reply) != reply_type)
    {
      if (message_type_of (reply) == MESSAGE_ID_NACK)
      {
        handle_nack (elvin, reply, error);
      } else
      {
        elvin_error_set
          (error, ELVIN_ERROR_PROTOCOL,
           "Unexpected reply from router: message ID %u",
           message_type_of (reply));
      }
    } else if (xid_of (request) != xid_of (reply))
    {
      elvin_error_set
        (error, ELVIN_ERROR_PROTOCOL,
         "Mismatched transaction ID in reply from router: %u (should be %u)",
         xid_of (reply), xid_of (request));
    }

    if (elvin_error_occurred (error))
      message_free (reply);
  }

  /* close connection on protocol error */
  if (error->code == ELVIN_ERROR_PROTOCOL)
    elvin_shutdown (elvin, REASON_PROTOCOL_VIOLATION, error->message);

  return elvin_error_ok (error);
}

bool send_message (Elvin *elvin, Message message, ElvinError *error)
{
  ByteBuffer buffer;
  size_t position = 0;
  uint32_t frame_size;

  byte_buffer_init (&buffer);

  on_error_return_false (byte_buffer_skip (&buffer, 4, error));

  message_write (&buffer, message, error);

  frame_size = (uint32_t)buffer.position - 4;

  /* write frame length */
  byte_buffer_set_position (&buffer, 0, error);
  byte_buffer_write_int32 (&buffer, frame_size, error);

  byte_buffer_set_position (&buffer, frame_size + 4, error);

  do
  {
    int bytes_written = send (elvin->socket, buffer.data + position,
                              buffer.position - position, 0);

    if (bytes_written == -1)
      elvin_error_from_errno (error);
    else
      position += bytes_written;
  } while (position < buffer.position && elvin_error_ok (error));

  byte_buffer_free (&buffer);

  return elvin_error_ok (error);
}

#ifdef WIN32
  #define sock_errno (WSAGetLastError ())
/*#define EWOULDBLOCK WSAEWOULDBLOCK */
#define sock_op_timed_out(err) (err == WSAETIMEDOUT)
#else
  #define sock_errno error
  #define sock_op_timed_out(err) (err == EAGAIN || err == EWOULDBLOCK)
#endif

bool receive_message (Elvin *elvin, Message message, ElvinError *error)
{
  ByteBuffer buffer;
  uint32_t frame_size;
  size_t position = 0;
  int bytes_read;

  bytes_read = recv (elvin->socket, (void *)&frame_size, 4, 0);

  if (bytes_read != 4)
  {
    if (sock_op_timed_out (sock_errno))
    {
      elvin_error_set (error, ELVIN_ERROR_TIMEOUT, 
                       "Receive timeout");
    } else
    {
      elvin_error_set (error, ELVIN_ERROR_PROTOCOL,
        "Failed to read router message: %u", sock_errno);
    }

    return false;
  }

  frame_size = ntohl (frame_size);

  if (frame_size == 0 || frame_size % 4 != 0 || frame_size > MAX_PACKET_LENGTH)
  {
    elvin_error_set
      (error, ELVIN_ERROR_PROTOCOL, "Illegal frame size: %lu", frame_size);

    return false;
  }

  byte_buffer_init_sized (&buffer, frame_size);

  do
  {
    bytes_read = recv (elvin->socket, buffer.data + position,
                       buffer.max_data_length - position, 0);

    if (bytes_read == -1)
      elvin_error_from_errno (error);
    else
      position += bytes_read;
  } while (position < buffer.max_data_length && elvin_error_ok (error));

  if (elvin_error_ok (error) && message_read (&buffer, message, error))
  {
    if (buffer.position < frame_size)
      elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Message underflow");
  }

  byte_buffer_free (&buffer);

  return elvin_error_ok (error);
}

Subscription *subscription_with_id (Elvin *elvin, uint64_t id)
{
  Subscription *subscription = elvin->subscriptions.items;
  size_t i;

  for (i = elvin->subscriptions.item_count; i > 0; i--, subscription++)
  {
    if (subscription->id == id)
      return subscription;
  }

  return NULL;
}

bool open_socket (Elvin *elvin, const char *host, uint16_t port,
                  ElvinError *error)
{
  struct addrinfo hints;
  struct addrinfo *info;
  struct addrinfo *i;
  int error_code;
  socket_t sock;
  char service [10];

  #ifdef WIN32
    on_error_return_false (init_windows_sockets (error));
  #endif

  elvin->socket = -1;

  snprintf (service, sizeof (service), "%u", port);

  memset (&hints, 0, sizeof (hints));
  hints.ai_family = PF_UNSPEC;
  hints.ai_socktype = SOCK_STREAM;

  if ((error_code = getaddrinfo (host, service, &hints, &info)))
  {
    return elvin_error_set (error, host_to_elvin_error (error_code),
                            gai_strerror (error_code));
  }

  for (i = info; i && elvin->socket == -1; i = i->ai_next)
  {
    sock = socket (i->ai_family, SOCK_STREAM, 0);

    if (sock != -1)
    {
      if (connect (sock, i->ai_addr, i->ai_addrlen) == 0)
      {
        /* set send/receive timeouts */ 
        #ifdef WIN32
          /* 
           * Windows seems to treat the seconds field as 
           * milliseconds from what I can see running tests.
           * Lord only knows what it thinks the microseconds
           * field is.
           */
          struct timeval timeout = {AVIS_IO_TIMEOUT, 0};
        #elif
          struct timeval timeout =
            {AVIS_IO_TIMEOUT / 1000, (AVIS_IO_TIMEOUT % 1000) * 1000};
        #endif

        setsockopt (sock, SOL_SOCKET, SO_RCVTIMEO,
                    (char *)&timeout, sizeof (timeout));
        setsockopt (sock, SOL_SOCKET, SO_SNDTIMEO,
                    (char *)&timeout, sizeof (timeout));

        elvin->socket = sock;
      } else
      {
        close_socket (sock);
      }
    }
  }

  freeaddrinfo (info);

  if (elvin->socket != -1)
    return true;
  else
    return elvin_error_from_errno (error);
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

#ifdef WIN32

void init_windows_sockets (ElvinError *error)
{
  WSADATA wsaData;
  int err;

  err = WSAStartup (MAKEWORD (2, 2), &wsaData);

  if (err != 0)
  {
    elvin_error_set (error, ELVIN_ERROR_INTERNAL,
                     "Failed to init winsock library");
  } else if (LOBYTE (wsaData.wVersion) != 2 ||
             HIBYTE (wsaData.wVersion) != 2)
  {
    WSACleanup ();

    elvin_error_set (error, ELVIN_ERROR_INTERNAL,
                     "Failed to find winsock 2.2");
  }
}

#endif
