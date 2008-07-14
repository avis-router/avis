#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include <elvin/elvin.h>

int main (int argc, const char * argv[]) 
{
  Elvin elvin;
  Elvin_Error error;
  
  if (!elvin_open (&elvin, "elvin://localhost", &error))
  {
    elvin_perror ("open", &error);
    exit (1);
  }
  
  elvin_close (&elvin);
  
  printf ("success!\n");
  
  return 0;
}
