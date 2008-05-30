#include <stdlib.h>
#include <string.h>

#include <elvin/elvin.h>
#include <elvin/elvin_uri.h>

static char *substring (const char *start, const char *end);

#define parse_fail(expr,message) \
  if (expr) \
  {\
    elvin_error_set (error, ELVIN_ERROR_INVALID_URI, message);\
    return false;\
  }

void elvin_uri_free (ElvinURI *uri)
{
  if (uri->host)
  {
    free (uri->host);
    
    uri->host = NULL;
  }
}

bool elvin_uri_from_string (ElvinURI *uri, const char *uri_string, 
                            ElvinError *error)
{
  const char *index1 = uri_string;
  const char *index2;
  
  uri->host = NULL;
  uri->port = DEFAULT_ELVIN_PORT;
  
  /* elvin://host*/
  /* elvin://host:port*/
  /* elvin:/xdr,none,ssl/host:port*/
  /* elvin:4.1/xdr,none,ssl/host:port*/
  /* elvin:4.1/xdr,none,ssl/host:port?n1=v1;n2=v2*/
  
  index2 = strchr (index1, ':');
  
  parse_fail (index2 == NULL, "No URI scheme present");
  parse_fail (memcmp ("elvin", index1, index2 - index1) != 0, 
              "Not an Elvin URI");
  
  /* first slash */
  index1 = strchr (index2 + 1, '/');
  parse_fail (index1 == NULL, "No host name present");
  
  if (index1 != index2 + 1)
  {
    /* TODO parse version */
  }

  /* second slash */
  index2 = strchr (index1 + 1, '/');
  parse_fail (index2 == NULL, "Missing second /");
  
  if (index2 != index1 + 1)
  {
    /* TODO parse protocol stack */
  }
  
  index1 = index2 + 1;
  
  parse_fail (*index1 == '\0', "Missing hostname");
  
  index2 = strchr (index1 + 1, '?');
  
  if (index2 == NULL)
  {
    uri->host = strdup (index1);
  } else
  {
    /* TODO parse name/values */
    
    uri->host = substring (index1, index2);
  }
  
  return true;
}

char *substring (const char *start, const char *end)
{
  size_t length = end - start;
  char *new_str = malloc (length + 1);
  
  memcpy (new_str, start, length);
  
  new_str [length] = '\0';
  
  return new_str;
}
