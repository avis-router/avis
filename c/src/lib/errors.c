#include <stdio.h>
#include <errno.h>
#include <string.h>

#include <elvin/stdtypes.h>
#include <elvin/errors.h>

void elvin_perror (const char *tag, ElvinError *error)
{
  printf ("%s: %s", tag, error->message);
}

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