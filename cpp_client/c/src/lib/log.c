#include <stdio.h>
#include <stdarg.h>

#include "log.h"

void elvin_log (int level, const char *message, ...)
{
  va_list args;
  
  va_start (args, message);
  
  /* todo */
  fprintf (stderr, "log: ");
  vfprintf (stderr, message, args);
  fprintf (stderr, "\n");
  
  va_end (args);
}