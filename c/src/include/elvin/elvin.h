/*
 *  elvin.h
 *
 *  Elvin Client Library
 *  Main Public Definitions.
 */

#ifndef ELVIN_H
#define ELVIN_H

#include <elvin/stdtypes.h>
#include <elvin/errors.h>

/** The default port for Elvin client connections. */
#define DEFAULT_ELVIN_PORT 2917

/* The default client protocol major version supported by this library. */
#define DEFAULT_CLIENT_PROTOCOL_MAJOR 4

/** The default client protocol minor version supported by this library. */
#define DEFAULT_CLIENT_PROTOCOL_MINOR 0

/*
 * A client's connection to an Elvin router.
 * 
 * See elvin_open() and elvin_open_url().
 */
typedef struct
{
  int socket;
} Elvin;

/**
 * A URL referring to an Elvin router.
 * 
 * See elvin_url_from_string() and elvin_open_url().
 */
typedef struct
{
  const char *host;
  uint16_t port;
} ElvinURI;

bool elvin_open (Elvin *elvin, const char *router_url, ElvinError *error);
bool elvin_open_url (Elvin *elvin, ElvinURI *url, ElvinError *error);
bool elvin_close (Elvin *elvin);

bool elvin_url_from_string (ElvinURI *url, const char *url_string, 
                            ElvinError *error);

#endif ELVIN_H
