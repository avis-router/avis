#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <assert.h>

#ifdef WIN32
  #include <winsock2.h>
  #include <ws2tcpip.h>
#else
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

#include "messages.h"
#include "arrays_private.h"
#include "log.h"

typedef struct
{
  SubscriptionListener listener;
  void *               user_data;
} SubscriptionListenerEntry;

static void elvin_shutdown (Elvin *elvin);

static bool open_socket (Elvin *elvin, const char *host, uint16_t port,
                         ElvinError *error);

static bool send_and_receive (Elvin *elvin, Message request, 
                              Message reply, MessageTypeID reply_type, 
                              ElvinError *error);

static bool send_message (Elvin *elvin, Message message, ElvinError *error);

static bool receive_message (Elvin *elvin, Message message, ElvinError *error);

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

bool elvin_open_with_keys (Elvin *elvin, ElvinURI *uri,
                           Keys *notification_keys, Keys *subscription_keys, 
                           ElvinError *error)
{
  alloc_message (conn_rqst);
  alloc_message (reply);
  
  elvin->socket = -1;
  array_list_init (&elvin->subscriptions, sizeof (Subscription), 5);
  elvin->notification_keys = notification_keys;
  elvin->subscription_keys = subscription_keys;
  
  if (!open_socket (elvin, uri->host, uri->port, error))
    return false;
  
  message_init (conn_rqst, MESSAGE_ID_CONN_RQST, 
                DEFAULT_CLIENT_PROTOCOL_MAJOR, 
                DEFAULT_CLIENT_PROTOCOL_MINOR,
                EMPTY_NAMED_VALUES, notification_keys, subscription_keys);
  
  on_error_return_false 
    (send_and_receive (elvin, conn_rqst, reply, MESSAGE_ID_CONN_RPLY, error));
  
  /* todo check message reply options */
  message_free (reply);
  
  return true;  
}

bool elvin_open_uri (Elvin *elvin, ElvinURI *uri, ElvinError *error)
{
  return elvin_open_with_keys (elvin, uri, EMPTY_KEYS, EMPTY_KEYS, error);
}

bool elvin_is_open (Elvin *elvin)
{
  return elvin->socket != -1;
}

bool elvin_close (Elvin *elvin)
{
  ElvinError error = elvin_error_create ();
  alloc_message (disconn_rqst);
  alloc_message (reply);
  
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
  Subscription *sub = elvin->subscriptions.items;
  
  if (elvin->socket == -1)
    return;
  
  #ifdef WIN32
    closesocket (elvin->socket);
  
    WSACleanup ();
  #else
    close (elvin->socket);
  #endif
  
  elvin->socket = -1;
  
  for (i = elvin->subscriptions.item_count; i > 0; i--, sub++)
    elvin_subscription_free (sub);
  
  array_list_free (&elvin->subscriptions);
 
  elvin_keys_destroy (elvin->notification_keys);
  elvin_keys_destroy (elvin->subscription_keys);
}

bool elvin_poll (Elvin *elvin, ElvinError *error)
{
  alloc_message (message); 
    
  if (receive_message (elvin, message, error))
  {
    switch (message_type_of (message))
    {
    case MESSAGE_ID_NOTIFY_DELIVER:
      handle_notify_deliver (elvin, message, error);
      break;
    case MESSAGE_ID_DISCONN:
      handle_disconn (elvin, message, error);
      break;
    default:
      elvin_error_set 
        (error, ELVIN_ERROR_PROTOCOL, 
         "Unexpected message type from router: %u", message_type_of (message));
    }
    
    message_free (message);
  }
  
  if (error->code == ELVIN_ERROR_PROTOCOL)
    elvin_shutdown (elvin);
  
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
  
  deliver_notification (elvin, secure_matches, attributes, true, error);
  
  if (elvin_error_occurred (error))
    return;
  
  deliver_notification (elvin, insecure_matches, attributes, false, error);
}

void deliver_notification (Elvin *elvin, Array *ids,
                           Attributes *attributes, bool secure, 
                           ElvinError *error)
{
  size_t i, j;
  int64_t *id = ids->items;
  SubscriptionListenerEntry *listener_entry;
  
  for (i = ids->item_count; i > 0; i--, id++)
  {
    Subscription *subscription = subscription_with_id (elvin, *id);
    
    if (subscription == NULL)
    {
      elvin_error_set (error, ELVIN_ERROR_PROTOCOL, 
                       "Invalid subscription ID from router: %llu", *id);

      return;
    }
    
    listener_entry = subscription->listeners.items;
    
    for (j = subscription->listeners.item_count; j > 0; j--, listener_entry++)
      (*listener_entry->listener) (subscription, attributes, secure, 
                                   listener_entry->user_data);
  }
}

bool elvin_send (Elvin *elvin, Attributes *notification, ElvinError *error)
{
  alloc_message (notify_emit);

  message_init (notify_emit, MESSAGE_ID_NOTIFY_EMIT, 
                notification, true, EMPTY_KEYS);
  
  return send_message (elvin, notify_emit, error);
}

Subscription *elvin_subscription_init (Subscription *subscription)
{
  subscription->elvin = NULL;
  subscription->id = 0;
  subscription->security = ALLOW_INSECURE_DELIVERY;
  subscription->keys = NULL;
  array_list_init (&subscription->listeners, 
                   sizeof (SubscriptionListenerEntry), 5);
  
  return subscription;
}

void elvin_subscription_free (Subscription *subscription)
{  
  free (subscription->subscription_expr);
  array_list_free (&subscription->listeners);
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
  alloc_message (sub_reply);
  
  message_init (sub_add_rqst, MESSAGE_ID_SUB_ADD_RQST, subscription_expr, 
                security, keys);
  
  if (send_and_receive (elvin, sub_add_rqst, sub_reply,
                        MESSAGE_ID_SUB_RPLY, error))
  {
    Subscription *subscription = 
      array_list_add (&elvin->subscriptions, Subscription);
    
    elvin_subscription_init (subscription);
    
    subscription->elvin = elvin;
    subscription->subscription_expr = strdup (subscription_expr);
    subscription->id = int64_at_offset (sub_reply, 4);
    subscription->keys = keys;

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
  alloc_message (sub_reply);
  bool succeeded;
  
  message_init (sub_del_rqst, MESSAGE_ID_SUB_DEL_RQST, subscription->id);
  
  succeeded = 
    send_and_receive (elvin, sub_del_rqst, sub_reply,
                      MESSAGE_ID_SUB_RPLY, error);
  
  elvin_subscription_free (subscription);

  array_list_remove_item_using_ptr 
    (&elvin->subscriptions, subscription, sizeof (Subscription));
  
  return succeeded;
}

/* TODO support adding general listeners */

void elvin_subscription_add_listener (Subscription *subscription, 
                                      SubscriptionListener listener,
                                      void *user_data)
{
  SubscriptionListenerEntry *entry = 
    array_list_add (&subscription->listeners, SubscriptionListenerEntry);

  entry->listener = listener;
  entry->user_data = user_data;
}

bool elvin_subscription_remove_listener (Subscription *subscription, 
                                         SubscriptionListener listener)
{
  SubscriptionListenerEntry *entry = subscription->listeners.items;
  int count;

  for (count = subscription->listeners.item_count; 
       count > 0 && entry->listener != listener; count--, entry++);
  
  if (count > 0)
  {
    array_list_remove_item_using_ptr (&subscription->listeners, entry, 
                                      sizeof (SubscriptionListenerEntry));
    
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
         "Mismatched transaction ID in reply from router: %u != %u", 
         xid_of (request), xid_of (reply));
    }
    
    if (elvin_error_occurred (error))
      message_free (reply);
  }
  
  /* close connection on protocol error */
  if (error->code == ELVIN_ERROR_PROTOCOL)
    elvin_shutdown (elvin);
  
  return elvin_error_ok (error);
}

bool send_message (Elvin *elvin, Message message, ElvinError *error)
{
  ByteBuffer buffer;
  size_t position = 0;
  
  byte_buffer_init (&buffer);
  
  message_write (&buffer, message, error);

  /* TODO set max size */

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

bool receive_message (Elvin *elvin, Message message, ElvinError *error)
{
  ByteBuffer buffer;
  uint32_t frame_size;
  size_t position = 0;
  int bytes_read;
    
  bytes_read = recv (elvin->socket, (void *)&frame_size, 4, 0);
  
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
    bytes_read = recv (elvin->socket, buffer.data + position, 
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
    return elvin_error_set (error, host_to_elvin_error (error_code), 
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