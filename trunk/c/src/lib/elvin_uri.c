#include <stdlib.h>
#include <string.h>

#include <avis/elvin.h>
#include <avis/elvin_uri.h>

static char *substring (const char *start, const char *end);
static const char *stranychr (const char *start, const char *chars);

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
  unsigned long port;
  
  uri->host = NULL;
  uri->port = DEFAULT_ELVIN_PORT;
  
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
  
  index2 = stranychr (index1 + 1, ":?");
  
  if (index2 == NULL)
  {
    uri->host = strdup (index1);
  } else
  {
    uri->host = substring (index1, index2);
    
    if (*index2 == ':')
    {
      index1 = index2 + 1;
      
      port = strtoul (index1, (char **)&index2, 10);
      
      parse_fail (index1 == index2 || port > 65535, "Invalid port number");
      
      uri->port = (uint16_t)port;
      
      index1 = index2;
    }
    
    if (*index1 == '?')
    {
      /* TODO parse name/values */
    }
  }
  
  return true;
}

/**
 * Like strchr(), but find the first occurrence of any character in chars.
 */
const char *stranychr (const char *start, const char *chars)
{
  const char *c;
  
  for ( ; *start; start++)
  {  
    for (c = chars; *c; c++)
    {
      if (*c == *start)
        return start;
    }   
  }
  
  return NULL;
}

/**
 * Create a substring between start (inclusive) and end (exclusive).
 */
char *substring (const char *start, const char *end)
{
  size_t length = end - start;
  char *new_str = malloc (length + 1);
  
  memcpy (new_str, start, length);
  
  new_str [length] = '\0';
  
  return new_str;
}
