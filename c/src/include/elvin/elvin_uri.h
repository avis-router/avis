#ifndef ELVIN_URI_H_
#define ELVIN_URI_H_

#include <elvin/stdtypes.h>
#include <elvin/errors.h>

/**
 * A URI referring to an Elvin router endpoint.
 * 
 * @see elvin_uri_from_string()
 * @see elvin_open_uri().
 */
typedef struct
{
  char *host;
  uint16_t port;
} ElvinURI;

/**
 * Parse a URI pointing to an Elvin router endpoint.
 * 
 * @param uri The URI to initialise.
 * @param uri_string The URI in text form.
 * @param error The error info.
 * 
 * Example URI's:
 * 
 * <pre>
 * elvin://host
 * elvin://host:port
 * elvin:/xdr,none,ssl/host:port
 * elvin:4.1/xdr,none,ssl/host:port
 * elvin:4.1/xdr,none,ssl/host:port?n1=v1;n2=v2
 * </pre>
 * 
 * @see elvin_uri_free()
 */
bool elvin_uri_from_string (ElvinURI *uri, const char *uri_string, 
                            ElvinError *error);

/**
 * Free any resources allocated to a URI.
 */
void elvin_uri_free (ElvinURI *uri);

#endif /*ELVIN_URI_H_*/
