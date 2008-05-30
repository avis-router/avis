#ifndef ELVIN_URI_H_
#define ELVIN_URI_H_

#include <elvin/stdtypes.h>
#include <elvin/errors.h>

/**
 * A URL referring to an Elvin router.
 * 
 * See elvin_url_from_string() and elvin_open_uri().
 */
typedef struct
{
  char *host;
  uint16_t port;
} ElvinURI;

bool elvin_uri_from_string (ElvinURI *uri, const char *uri_string, 
                            ElvinError *error);

void elvin_uri_free (ElvinURI *uri);

#endif /*ELVIN_URI_H_*/
