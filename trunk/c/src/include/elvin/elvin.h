/*
 *  elvin.h
 *
 *  Elvin Client Library
 *  Main Public Definitions.
 */

#ifndef ELVIN_H
#define ELVIN_H

#include <stdbool.h>
#include <stdint.h>

#include <elvin/errors.h>

#define DEFAULT_ELVIN_PORT 2917
#define DEFAULT_CLIENT_PROTOCOL_MAJOR 4
#define DEFAULT_CLIENT_PROTOCOL_MINOR 0

typedef struct Elvin_t
{
  int socket;
} Elvin;

typedef struct Elvin_URL_t
{
  const char *host;
  uint16_t port;
} Elvin_URL;

bool elvin_open (Elvin *elvin, const char *router_url, Elvin_Error *error);
bool elvin_open_url (Elvin *elvin, Elvin_URL *url, Elvin_Error *error);
bool elvin_close (Elvin *elvin);

bool elvin_url_from_string (Elvin_URL *url, const char *url_string, 
                            Elvin_Error *error);

#endif ELVIN_H
