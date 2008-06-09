#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <errno.h>
#include <string.h>

#include <avis/stdtypes.h>
#include <avis/errors.h>
#include <avis/log.h>

void elvin_error_free (ElvinError *error)
{
  if (error->message)
  {
    free (error->message);
    
    error->message = NULL;
  }
  
  error->code = ELVIN_ERROR_NONE;
}

void elvin_perror (const char *tag, ElvinError *error)
{
  if (error->message == NULL)
    fprintf (stderr, "elvin_perror () called with no message\n");
  else
    fprintf (stderr, "%s: %s\n", tag, error->message);
}

bool elvin_error_set (ElvinError *error, int code, const char *message, ...)
{
  int chars_written, message_length = 300;
  char *np;
  va_list args;
  
  /* do not allow earlier error to override */
  if (error->code != ELVIN_ERROR_NONE)
  {
    DIAGNOSTIC1 ("Ignoring error override: %s", message);
    
    return false;
  }
  
  error->code = code;

  /* generate a formatted message, allowing for expanding space */
  error->message = malloc (message_length);
 
  while (error->message != NULL)
  {
    /* Try to print in the allocated space. */
    va_start (args, message);
    
    chars_written = vsnprintf (error->message, message_length, message, args);
    
    va_end (args);
   
    /* If that worked, done */
    if (chars_written > -1 && chars_written < message_length)
      break;
    
    /* Else try again with more space. */
    if (chars_written > -1) /* glibc 2.1 */
      message_length = chars_written + 1; /* precisely what is needed */
    else
      /* glibc 2.0 */
      message_length *= 2; /* twice the old message_length */
   
    if ((np = realloc (error->message, message_length)) == NULL)
    {
      free (error->message);
     
      error->message = NULL;
    } else
    {
      error->message = np;
    }
  }
  
  return false;
}

bool elvin_error_from_errno (ElvinError *error)
{
  error->code = errno;
  error->message = strdup (strerror (errno));
  
  return false;
}

bool elvin_error_assert (ElvinError *error, bool condition, 
                         int code, const char *message)
{
  if (condition)
    return true;
  else
    return elvin_error_set (error, code, message);
}