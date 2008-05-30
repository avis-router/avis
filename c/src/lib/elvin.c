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
  #include <netdb.h>
  #include <sys/socket.h>
  #include <sys/types.h>
  #include <netinet/in.h>
  #include <arpa/inet.h>
#endif

#include <elvin/stdtypes.h>
#include <elvin/elvin.h>
#include <elvin/errors.h>
#include <elvin/log.h>

#include "messages.h"

static bool open_socket (Elvin *elvin, const char *host, uint16_t port,
                         ElvinError *error);

static Message send_and_receive (socket_t socket, Message request, 
                                 MessageTypeID reply_type, ElvinError *error);

static bool send_message (socket_t sockfd, Message message, ElvinError *error);

static Message receive_message (socket_t socket, ElvinError *error);

static bool resolve_address (struct sockaddr_in *router_addr,
                             const char *host, uint16_t port, 
                             ElvinError *error);

#ifdef WIN32
  static void init_windows_sockets (ElvinError *error);
#endif

bool elvin_open (Elvin *elvin, const char *router_url, ElvinError *error)
{
  ElvinURI url;
  
  if (!elvin_url_from_string (&url, router_url, error))
    return false;

  return elvin_open_uri (elvin, &url, error);
}

bool elvin_open_uri (Elvin *elvin, ElvinURI *url, ElvinError *error)
{
  Message connRqst;
  Message reply;
  
  elvin->socket = -1;
  
  if (!open_socket (elvin, url->host, url->port, error))
    return false;
  
  connRqst = message_alloca ();
  
  message_init (connRqst, MESSAGE_ID_CONN_RQST, 
                DEFAULT_CLIENT_PROTOCOL_MAJOR, 
                DEFAULT_CLIENT_PROTOCOL_MINOR,
                EMPTY_NAMED_VALUES, EMPTY_KEYS, EMPTY_KEYS);
  
  on_error_return_false 
    (reply = send_and_receive (elvin->socket, connRqst, 
                               MESSAGE_ID_CONN_RPLY, error));
  
  /* todo check message reply options */
  message_destroy (reply);
  
  return true;
}

bool elvin_close (Elvin *elvin)
{
  ElvinError error = elvin_error_create ();
  Message disconnRqst;
  Message reply;
  
  if (elvin->socket == -1)
    return false;
  
  disconnRqst = message_alloca ();
  message_init (disconnRqst, MESSAGE_ID_DISCONN_RQST);
  
  reply = 
    send_and_receive (elvin->socket, disconnRqst, 
                      MESSAGE_ID_DISCONN_RPLY, &error);

  if (reply)
    message_destroy (reply);

  #ifdef WIN32
    closesocket (elvin->socket);

    WSACleanup ();
  #else
    close (elvin->socket);
  #endif

  elvin->socket = -1;
  
  elvin_error_destroy (&error);
  
  return true;
}

bool elvin_url_from_string (ElvinURI *url, const char *url_string, 
                            ElvinError *error)
{
  /* TODO */
  url->host = "localhost";
  url->port = DEFAULT_ELVIN_PORT;
  
  return true;
}

static bool open_socket (Elvin *elvin, const char *host, uint16_t port,
                         ElvinError *error)
{
  struct sockaddr_in router_addr;
  socket_t sockfd;
  
  #ifdef WIN32
    on_error_return_false (init_windows_sockets (error));
  #endif  
  
  on_error_return_false (resolve_address (&router_addr, host, port, error));
  
  sockfd = socket (PF_INET, SOCK_STREAM, 0);
  
  if (sockfd == -1)
    return elvin_error_from_errno (error);
  
  if (connect (sockfd, (struct sockaddr *)&router_addr, 
               sizeof (router_addr)) == 0)
  {
    elvin->socket = sockfd;
    
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

Message send_and_receive (socket_t socket, Message request, 
                          MessageTypeID reply_type, ElvinError *error)
{
  Message reply;
  
  /* todo could share the buffer for this */
  if (!send_message (socket, request, error))
    return NULL;
  
  reply = receive_message (socket, error);
  
  if (!reply)
    return NULL;
  
  if (message_type_of (reply) != reply_type)
  {
    /* todo handle NACK properly */
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, 
                     "Unexpected reply from router");
  } else if (xid_of (request) != xid_of (reply))
  {
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, 
                     "Mismatched transaction ID in reply from router");
  }
  
  if (elvin_error_occurred (error))
  {
    message_free (reply);
    
    reply = NULL;
  }
  
  return reply;
}

bool send_message (socket_t socket, Message message, ElvinError *error)
{
  ByteBuffer buffer;
  uint32_t position = 0;
  
  byte_buffer_init (&buffer);
  
  message_write (&buffer, message, error);

  /* todo set max size */

  while (position < buffer.position && elvin_error_ok (error))
  {
    size_t bytes_written = send (socket, buffer.data + position, 
                                 buffer.position - position, 0);
    
    if (bytes_written == -1)
      elvin_error_from_errno (error);
    else
      position += bytes_written;      
  }
  
  byte_buffer_free (&buffer);
  
  return elvin_error_ok (error);
}

Message receive_message (socket_t socket, ElvinError *error)
{
  ByteBuffer buffer;
  Message message = NULL;
  uint32_t frame_size;
  uint32_t position = 0;
  size_t bytes_read;
    
  bytes_read = recv (socket, (void *)&frame_size, 4, 0);
  
  if (bytes_read != 4)
  {
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, 
                     "Failed to read frame size");
    
    return NULL;
  }
  
  frame_size = ntohl (frame_size);

  /* todo check size is not too big */
  byte_buffer_init_sized (&buffer, frame_size);

  while (position < buffer.max_data_length && elvin_error_ok (error))
  {
    bytes_read = recv (socket, buffer.data + position, 
                       buffer.max_data_length - position, 0);
   
    if (bytes_read == -1)
      elvin_error_from_errno (error);
    else
      position += bytes_read;
  }

  if (elvin_error_ok (error))
    message = message_read (&buffer, error);
  
  byte_buffer_free (&buffer);

  return message;
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
