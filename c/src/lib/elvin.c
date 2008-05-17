#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <netdb.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <elvin/elvin.h>
#include <elvin/errors.h>

#include "messages.h"

static bool open_socket (Elvin *elvin, const char *host, uint16_t port,
                         Elvin_Error *error);

static void *send_and_receive (int socket, void *request_message, 
                               Message_Id reply_type, Elvin_Error *error);

static bool send_message (int sockfd, void *message, Elvin_Error *error);

static void *receive_message (int socket, Elvin_Error *error);

bool elvin_open (Elvin *elvin, const char *router_url, Elvin_Error *error)
{
  Elvin_URL url;
  
  if (!elvin_url_from_string (&url, router_url, error))
    return false;

  return elvin_open_url (elvin, &url, error);
}

bool elvin_open_url (Elvin *elvin, Elvin_URL *url, Elvin_Error *error)
{
  elvin->socket = -1;
  
  if (!open_socket (elvin, url->host, url->port, error))
    return false;
  
  ConnRqst connRqst;
  
  ConnRqst_init (&connRqst, DEFAULT_CLIENT_PROTOCOL_MAJOR, 
                 DEFAULT_CLIENT_PROTOCOL_MINOR,
                 EMPTY_NAMED_VALUES, EMPTY_KEYS, EMPTY_KEYS);
  
  ConnRply *reply;
  
  error_return 
    (reply = send_and_receive (elvin->socket, &connRqst, 
                               MESSAGE_ID_CONN_RPLY, error));
  
  // todo check message reply options
  message_destroy (reply);
  
  return true;
}

bool elvin_close (Elvin *elvin)
{
  if (elvin->socket == -1)
    return false;
  
  Elvin_Error error = elvin_error_create ();
  
  DisconnRqst disconnRqst;
  DisconnRqst_init (&disconnRqst);
  
  DisconnRply *reply;
    
  reply = send_and_receive (elvin->socket, &disconnRqst, 
                            MESSAGE_ID_DISCONN_RPLY, &error);

  if (reply)
    message_destroy (reply);

  close (elvin->socket); // TODO use closesocket () for Windows

  elvin->socket = -1;
  
  elvin_error_destroy (&error);
  
  return true;
}

bool elvin_url_from_string (Elvin_URL *url, const char *url_string, 
                            Elvin_Error *error)
{
  // TODO
  url->host = "localhost";
  url->port = DEFAULT_ELVIN_PORT;
  
  return true;
}

static bool open_socket (Elvin *elvin, const char *host, uint16_t port,
                         Elvin_Error *error)
{
  struct hostent *host_info = gethostbyname (host);
  
  if (host_info == NULL) 
  {
    return elvin_error_set (error, 
                            HOST_TO_ELVIN_ERROR (h_errno), hstrerror (h_errno));
  }
  
  struct sockaddr_in router_addr;
  router_addr.sin_family = AF_INET;
  router_addr.sin_port = htons (port);
  router_addr.sin_addr = *((struct in_addr *)host_info->h_addr);
  memset (router_addr.sin_zero, '\0', sizeof router_addr.sin_zero);
  
  int sockfd = socket (PF_INET, SOCK_STREAM, 0);
  
  if (sockfd == -1)
    return elvin_error_from_errno (error);
  
  if (connect (sockfd, (struct sockaddr *)&router_addr, 
               sizeof router_addr) == 0)
  {
    elvin->socket = sockfd;
    
    return true;
  } else
  {
    return elvin_error_from_errno (error);
  }
}

void *send_and_receive (int socket, void *request_message, 
                        Message_Id reply_type, Elvin_Error *error)
{
  // todo could share the buffer for this
  if (!send_message (socket, request_message, error))
    return NULL;
  
  void *reply = receive_message (socket, error);
  
  if (!reply)
    return NULL;
  
  if (message_type_of (reply) != reply_type)
  {
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, 
                     "Unexpected reply from router");
  } else if (xid_of (request_message) != xid_of (reply))
  {
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, 
                     "Mismatched transaction ID in reply from router");
  }
  
  if (elvin_error_occurred (error))
  {
    message_destroy (reply);
    
    reply = NULL;
  }
  
  return reply;
}

bool send_message (int socket, void *message, Elvin_Error *error)
{
  Byte_Buffer *buffer = byte_buffer_create ();
  
  // todo set max size
  
  error_return (message_write (buffer, message, error));
  
  size_t position = 0;

  while (position < buffer->position)
  {
    size_t written = send (socket, buffer->data + position, 
                           buffer->position - position, 0);
    
    if (written == -1)
    {
      elvin_error_from_errno (error);
      
      break;
    } else
    {
      position += written;      
    }
  }
  
  byte_buffer_destroy (buffer);
  
  return elvin_error_ok (error);
}

void *receive_message (int socket, Elvin_Error *error)
{
  uint32_t frame_size;
  
  size_t bytes_read = recv (socket, &frame_size, 4, 0);
    
  if (bytes_read != 4)
  {
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Failed to read frame size");
    
    return NULL;
  }
  
  // todo check size is not too big
  frame_size = ntohl (frame_size);

  Byte_Buffer *buffer = byte_buffer_create_sized (frame_size);
  
  size_t position = 0;
  void *message = NULL;
  
  while (position < buffer->max_data_length)
  {
    bytes_read = 
      recv (socket, buffer->data + position, 
            buffer->max_data_length - position, 0);
   
    if (bytes_read == -1)
    {
      elvin_error_from_errno (error);
      
      break;
    } else
    {
      position += bytes_read;
    }
  }

  if (elvin_error_ok (error))
    message_read (buffer, &message, error);

  return message;
}