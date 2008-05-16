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
  
  ConnRqst *connRqst = 
      ConnRqst_create (DEFAULT_CLIENT_PROTOCOL_MAJOR, 
                       DEFAULT_CLIENT_PROTOCOL_MINOR,
                       EMPTY_NAMED_VALUES, EMPTY_KEYS, EMPTY_KEYS);
  
  bool sent = send_message (elvin->socket, connRqst, error);
  
  ConnRqst_destroy (connRqst);
  
  void *reply = receive_message (elvin->socket, error);
  
  if (!reply)
    return false;
  
  return sent;
}

bool elvin_close (Elvin *elvin)
{
  if (elvin->socket == -1)
    return false;
  
  close (elvin->socket); // TODO use closesocket () for Windows

  elvin->socket = -1;
  
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

bool send_message (int socket, void *message, Elvin_Error *error)
{
  Byte_Buffer *buffer = byte_buffer_create ();
  
  // todo set max size
  
  error_return (message_write (buffer, message, error));
  
  bool success = false;
  size_t position = 0;

  for (;;)
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
      
      if (position >= buffer->position)
      {
        success = true;
        break;
      }
    }
  }
  
  byte_buffer_destroy (buffer);
  
  return success;
}

void *receive_message (int socket, Elvin_Error *error)
{
  // todo move frame length code out of message_read
  
  Byte_Buffer *buffer = byte_buffer_create ();

  uint32_t frame_size;
  
  size_t bytes_read = recv (socket, &frame_size, 4, 0);
    
  if (bytes_read != 4)
  {
    elvin_error_set (error, ELVIN_ERROR_PROTOCOL, "Failed to read frame size");
    
    return NULL;
  }
  
  // todo check size
  frame_size = ntohl (frame_size);

  byte_buffer_set_max_length (buffer, frame_size + 4);
  byte_buffer_ensure_capacity (buffer, frame_size + 4);
  byte_buffer_write_int32 (buffer, frame_size, error);
  
  size_t position = buffer->position;
  void *message = NULL;
  
  for (;;)
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
      
      if (position >= buffer->max_data_length)
      {
        byte_buffer_set_position (buffer, 0, error);
        
        message_read (buffer, &message, error);
        
        break;
      }
    }
  }
  
  return message;
}