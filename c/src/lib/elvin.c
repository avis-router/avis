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
#include "messages.h"

static bool open_socket (Elvin *elvin, const char *host, uint16_t port,
                         Elvin_Error *error);

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
  
/*  ConnRqst connRqst;
  connRqst->init (&connRqst);
  
  send_packet (connRqst); 
  
  connRqst->destroy (&connRqst);*/
  
  return true;
}

bool elvin_close (Elvin *elvin)
{
  if (elvin->socket == -1)
    return false;
  
//  if (elvin_error_assert (elvin->socket != 0, ELVIN_ERROR_CONNECTION_CLOSED, 
//                          "Socket not open"))
//  {
//    return false;
//  }
  
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

void elvin_perror (const char *tag, Elvin_Error *error)
{
  printf ("%s: %s", tag, error->message);
}

bool elvin_error_set (Elvin_Error *error, int code, const char *message)
{
  error->code = code;
  error->message = message;
  
  return false;
}

bool elvin_error_from_errno (Elvin_Error *error)
{
  error->code = errno;
  error->message = strerror (errno);
  
  return false;
}

bool elvin_error_assert (Elvin_Error *error, bool condition, 
                         int code, const char *message)
{
  if (condition)
  { 
    return true;
  } else
  {
    error->message = message;
    error->code = code;
    
    return false;
  }
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