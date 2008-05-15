#include <stdio.h>
#include <stdbool.h>
#include <errno.h>
#include <string.h>

#include <elvin/errors.h>

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
  error->message = (const char *)strerror (errno);
  
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