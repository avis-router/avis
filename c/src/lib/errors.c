#include <stdio.h>
#include <errno.h>
#include <string.h>

#include <avis/stdtypes.h>
#include <avis/errors.h>

void elvin_perror (const char *tag, ElvinError *error)
{
  if (error->message == NULL)
    fprintf (stderr, "elvin_perror () called with no message\n");
  else
    printf ("%s: %s\n", tag, error->message);
}

/* TODO should this allow later errors to override earlier ones? */
bool elvin_error_set (ElvinError *error, int code, const char *message)
{
  error->code = code;
  error->message = message;
  
  return false;
}

bool elvin_error_from_errno (ElvinError *error)
{
  error->code = errno;
  error->message = (const char *)strerror (errno);
  
  return false;
}

bool elvin_error_assert (ElvinError *error, bool condition, 
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