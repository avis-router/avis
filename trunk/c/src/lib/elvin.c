#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <assert.h>

#ifdef WIN32
  #include <winsock2.h>
  #include <ws2tcpip.h>
#else

  /* For Linux */
  #define __USE_POSIX

  #include <unistd.h>
  #include <sys/types.h>
  #include <sys/socket.h>
  #include <netdb.h>
  #include <netinet/in.h>
  #include <arpa/inet.h>
#endif

#include <avis/stdtypes.h>
#include <avis/elvin.h>
#include <avis/errors.h>
#include <avis/values.h>
#include <avis/log.h>

#include "messages.h"
#include "array_list_private.h"

static void elvin_shutdown (Elvin *elvin);

static bool open_socket (Elvin *elvin, const char *host, uint16_t port,
                         ElvinError *error);

static bool send_and_receive (Elvin *elvin, Message request, 
                              Message reply, MessageTypeID reply_type, 
                              ElvinError *error);

static bool send_message (socket_t socketfd, Message message, ElvinError *error);

static bool receive_message (socket_t socketfd, Message message, 
                             ElvinError *error);

static bool resolve_address (struct sockaddr_in *router_addr,
                             const char *host, uint16_t port, 
                             ElvinError *error);

static void handle_notify_deliver (Elvin *elvin, Message message, 
                                   ElvinError *error);

static void handle_nack (Elvin *elvin, Message message, ElvinError *error);

static void handle_disconn (Elvin *elvin, Message message, ElvinError *error);

#define handle_protocol_error(elvin, error, message) \
  elvin_error_set (error, ELVIN_ERROR_PROTOCOL, message), \
  elvin_shutdown (elvin)

#define handle_protocol_error1(elvin, error, message, arg1) \
  elvin_error_set (error, ELVIN_ERROR_PROTOCOL, message, arg1), \
  elvin_shutdown (elvin)

#define handle_protocol_error2(elvin, error, message, arg1, arg2) \
  elvin_error_set (error, ELVIN_ERROR_PROTOCOL, message, arg1, arg2), \
  elvin_shutdown (elvin)

static Subscription *subscription_with_id (Elvin *elvin, uint64_t id);

static void deliver_notification (Elvin *elvin, Array *ids, 
                                  Notification *notification,
                                  ElvinError *error);

static Subscription *elvin_subscription_create ();

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

bool elvin_open_uri (Elvin *elvin, ElvinURI *url, ElvinError *error)
{
  Message conn_rqst = message_alloca ();
  Message reply = message_alloca ();
  
  elvin->socket = -1;
  array_list_init (&elvin->subscriptions, sizeof (Subscription *), 5);
  
  if (!open_socket (elvin, url->host, url->port, error))
    return false;
  
  message_init (conn_rqst, MESSAGE_ID_CONN_RQST, 
                DEFAULT_CLIENT_PROTOCOL_MAJOR, 
                DEFAULT_CLIENT_PROTOCOL_MINOR,
                EMPTY_NAMED_VALUES, EMPTY_KEYS, EMPTY_KEYS);
  
  on_error_return_false 
    (send_and_receive (elvin, conn_rqst, reply, MESSAGE_ID_CONN_RPLY, error));
  
  /* todo check message reply options */
  message_free (reply);
  
  return true;
}

bool elvin_is_open (Elvin *elvin)
{
  return elvin->socket != -1;
}

bool elvin_close (Elvin *elvin)
{
  ElvinError error = elvin_error_create ();
  Message disconn_rqst = message_alloca ();
  Message reply = message_alloca ();
  
  if (elvin->socket == -1)
    return false;
  
  message_init (disconn_rqst, MESSAGE_ID_DISCONN_RQST);
  
  send_and_receive (elvin, disconn_rqst, reply,
                    MESSAGE_ID_DISCONN_RPLY, &error);

  elvin_shutdown (elvin);
  
  elvin_error_free (&error);
  
  return true;
}

void elvin_shutdown (Elvin *elvin)
{
  size_t i;
  Subscription **sub = elvin->subscriptions.items;
  
  #ifdef WIN32
    closesocket (elvin->socket);
  
    WSACleanup ();
  #else
    close (elvin->socket);
  #endif
  
  elvin->socket = -1;
  
  for (i = elvin->subscriptions.item_count; i > 0; i--, sub++)
  {
    elvin_subscription_free (*sub);
    free (*sub);
  }
  
  array_list_free (&elvin->subscriptions);
}

bool elvin_poll (Elvin *elvin, ElvinError *error)
{
  Message message = message_alloca (); 
    
  if (!receive_message (elvin->socket, message, error))
    return false;
  
  switch (message_type_of (message))
  {
  case MESSAGE_ID_NOTIFY_DELIVER:
    handle_notify_deliver (elvin, message, error);
    break;
  case MESSAGE_ID_DISCONN:
    handle_disconn (elvin, message, error);
    break;
  default:
    handle_protocol_error1 
      (elvin, error, "Unexpected message type from router: %u", 
       message_type_of (message));
    
    break;
  }
  
  message_free (message);
  
  return elvin_error_ok (error);
}

void handle_disconn (Elvin *elvin, Message message, ElvinError *error)
{
  /* TODO support close listener */
  elvin_shutdown (elvin);
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
    handle_protocol_error1 (elvin, error, "Unexpected NACK from router: %s", 
                            message);
  }
}

void handle_notify_deliver (Elvin *elvin, Message message, ElvinError *error)
{
  Attributes *attributes = ptr_at_offset (message, 0);
  Array *secure_matches = 
    ptr_at_offset (message, sizeof (Attributes *));
  Array *insecure_matches = 
    ptr_at_offset (message, sizeof (Attributes *) + sizeof (Array *));
  Notification notification;
  
  notification.attributes = *attributes;
  notification.secure = true;
  
  deliver_notification (elvin, secure_matches, &notification, error);
  
  if (elvin_error_occurred (error))
    return;
  
  notification.secure = false;
  deliver_notification (elvin, insecure_matches, &notification, error);
}

void deliver_notification (Elvin *elvin, Array *ids,
                           Notification *notification, 
                           ElvinError *error)
{
  size_t i, j;
  int64_t *id = ids->items;
  SubscriptionListener *listeners;
  
  for (i = ids->item_count; i > 0; i--, id++)
  {
    Subscription *subscription = subscription_with_id (elvin, *id);
    
    if (subscription == NULL)
    {
      handle_protocol_error1 
        (elvin, error, "Invalid subscription ID from router: %lu", *id);
      
      return;
    }
    
    listeners = subscription->listeners.items;
    
    for (j = subscription->listeners.item_count; j > 0; j--, listeners++)
      (*listeners) (subscription, notification);
  }
}

bool elvin_send (Elvin *elvin, Attributes *notification, ElvinError *error)
{
  Message notify_emit = message_alloca ();

  message_init (notify_emit, MESSAGE_ID_NOTIFY_EMIT, 
                notification, true, EMPTY_KEYS);
  
  return send_message (elvin->socket, notify_emit, error);
}

#define elvin_subscription_destroy(sub) \
  (elvin_subscription_free (sub), free (sub), sub = NULL)

Subscription *elvin_subscription_create (const char *subscription_expr)
{
  Subscription *subscription = malloc (sizeof (Subscription));
  
  subscription->elvin = NULL;
  subscription->id = 0;
  subscription->security = ALLOW_INSECURE_DELIVERY;
  subscription->subscription_expr = subscription_expr;
  array_list_init (&subscription->listeners, sizeof (SubscriptionListener), 5);
  elvin_keys_init (&subscription->keys);
  
  return subscription;
}

void elvin_subscription_free (Subscription *subscription)
{
  subscription->elvin = NULL;
  subscription->id = 0;
  array_list_free (&subscription->listeners);
  elvin_keys_free (&subscription->keys);
}

Subscription *elvin_subscribe (Elvin *elvin, const char *subscription_expr, 
                               ElvinError *error)
{
  Subscription *subscription = elvin_subscription_create (subscription_expr);
  Message sub_add_rqst = message_alloca ();
  Message sub_reply = message_alloca ();
  
  message_init (sub_add_rqst,
                MESSAGE_ID_SUB_ADD_RQST, subscription->subscription_expr,
                ALLOW_INSECURE_DELIVERY,
                &subscription->keys);
  
  if (send_and_receive (elvin, sub_add_rqst, sub_reply,
                        MESSAGE_ID_SUB_RPLY, error))
  {
    subscription->id = int64_at_offset (sub_reply, 4);
    
    array_list_add_ptr (&elvin->subscriptions, subscription);

    return subscription;
  } else
  {
    elvin_subscription_destroy (subscription);
    
    return NULL;
  }
}

bool elvin_unsubscribe (Elvin *elvin, Subscription *subscription, 
                        ElvinError *error)
{
  Message sub_del_rqst = message_alloca ();
  Message sub_reply = message_alloca ();
  bool succeeded;
  
  message_init (sub_del_rqst, MESSAGE_ID_SUB_DEL_RQST, subscription->id);
  
  succeeded = 
    send_and_receive (elvin, sub_del_rqst, sub_reply,
                      MESSAGE_ID_SUB_RPLY, error);
  
  array_list_remove_ptr (&elvin->subscriptions, subscription);

  elvin_subscription_destroy (subscription);
  
  return succeeded;
}

/* TODO support adding general listeners */
/* TODO support client data pointer */

void elvin_subscription_add_listener (Subscription *subscription, 
                                      SubscriptionListener listener)
{
  array_list_add_func (&subscription->listeners, listener);
}

bool send_and_receive (Elvin *elvin, Message request, 
                       Message reply, MessageTypeID reply_type, 
                       ElvinError *error)
{
  /* TODO could share the buffer for this */
  if (!(send_message (elvin->socket, request, error) && 
        receive_message (elvin->socket, reply, error)))
  {
    handle_protocol_error (elvin, error, error->message);
    
    return false;
  }
  
  if (xid_of (request) != xid_of (reply))
  {
    handle_protocol_error2 
      (elvin, error, "Mismatched transaction ID in reply from router: %u != %u", 
       xid_of (request), xid_of (reply));
  } else if (message_type_of (reply) != reply_type)
  {
    if (message_type_of (reply) == MESSAGE_ID_NACK)
    {
      handle_nack (elvin, reply, error);
    } else
    {
      handle_protocol_error1 
        (elvin, error, "Unexpected reply from router: message ID %u", 
        message_type_of (reply));
    }
  }
  
  if (elvin_error_ok (error))
  {
    return true;
  } else
  {
    message_free (reply);
    
    return false;
  }
}

bool send_message (socket_t socketfd, Message message, ElvinError *error)
{
  ByteBuffer buffer;
  size_t position = 0;
  
  byte_buffer_init (&buffer);
  
  message_write (&buffer, message, error);

  /* TODO set max size */

  do
  {
    int bytes_written = send (socketfd, buffer.data + position, 
							                buffer.position - position, 0);
    
    if (bytes_written == -1)
      elvin_error_from_errno (error);
    else
      position += bytes_written;      
  } while (position < buffer.position && elvin_error_ok (error));
  
  byte_buffer_free (&buffer);
  
  return elvin_error_ok (error);
}

bool receive_message (socket_t socketfd, Message message, ElvinError *error)
{
  ByteBuffer buffer;
  uint32_t frame_size;
  size_t position = 0;
  int bytes_read;
    
  bytes_read = recv (socketfd, (void *)&frame_size, 4, 0);
  
  if (bytes_read != 4)
  {
    return elvin_error_set (error, ELVIN_ERROR_PROTOCOL, 
                            "Failed to read router message");
  }
  
  frame_size = ntohl (frame_size);

  /* TODO check size is not too big or < 4 */
  byte_buffer_init_sized (&buffer, frame_size);

  do
  {
    bytes_read = recv (socketfd, buffer.data + position, 
                       buffer.max_data_length - position, 0);
   
    if (bytes_read == -1)
      elvin_error_from_errno (error);
    else
      position += bytes_read;
  } while (position < buffer.max_data_length && elvin_error_ok (error));

  if (elvin_error_ok (error))
    message_read (&buffer, message, error);
  
  byte_buffer_free (&buffer);

  return elvin_error_ok (error);
}

Subscription *subscription_with_id (Elvin *elvin, uint64_t id)
{
  Subscription **subscriptions = elvin->subscriptions.items;
  size_t i;
 
  for (i = elvin->subscriptions.item_count; i > 0; i--, subscriptions++)
  {
    if ((*subscriptions)->id == id)
      return *subscriptions;
  }
  
  return NULL;
}

bool open_socket (Elvin *elvin, const char *host, uint16_t port,
                  ElvinError *error)
{
  struct sockaddr_in router_addr;
  
  #ifdef WIN32
    on_error_return_false (init_windows_sockets (error));
  #endif  
  
  on_error_return_false (resolve_address (&router_addr, host, port, error));
  
  elvin->socket = socket (PF_INET, SOCK_STREAM, 0);
  
  if (elvin->socket != -1 && 
      connect (elvin->socket, (struct sockaddr *)&router_addr, 
               sizeof (router_addr)) == 0)
  {
    return true;
  } else
  {
    return elvin_error_from_errno (error);
  }
}

bool resolve_address (struct sockaddr_in *router_addr,
                      const char *host, uint16_t port, 
                      ElvinError *error)
{
  struct addrinfo hints;
  struct addrinfo *address_info;
  int error_code;
  
  /* TODO this does not appear to work with IPv6 */
  memset (&hints, '\0', sizeof (struct addrinfo));
  hints.ai_family = AF_INET;
  hints.ai_socktype = SOCK_STREAM;

  if ((error_code = getaddrinfo (host, NULL, &hints, &address_info)) != 0)
  {
    return elvin_error_set (error, HOST_TO_ELVIN_ERROR (error_code), 
                            gai_strerror (error_code));
  }
  
  memcpy (router_addr, address_info->ai_addr, address_info->ai_addrlen);  
  memset (router_addr->sin_zero, '\0', sizeof (router_addr->sin_zero));
  router_addr->sin_port = htons (port);
    
  #if LOGGING (LOG_LEVEL_DIAGNOSTIC)
  {
    char ip [46];

    inet_ntop (address_info->ai_family, &router_addr->sin_addr, 
               ip, sizeof (ip));
    DIAGNOSTIC2 ("Resolved router address %s = %s\n", host, ip);
  }
  #endif
  
  freeaddrinfo (address_info);

  return true;
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
