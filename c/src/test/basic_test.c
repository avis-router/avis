#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include <elvin/elvin.h>

int main (int argc, const char * argv[]) 
{
  Elvin elvin;
  ElvinError error = elvin_error_create ();
  NamedValues *ntfn;

  if (!elvin_open (&elvin, "elvin://localhost", &error))
  {
    elvin_perror ("open", &error);
    exit (1);
  }

  ntfn = named_values_create ();
    
  named_values_set_int32 (ntfn, "favourite number", 42);
  named_values_set_string (ntfn, "some text", "paydirt");
  
  elvin_send (&elvin, ntfn, &error);

  named_values_destroy (ntfn);
  
  elvin_close (&elvin);
  
  if (elvin_error_ok (&error))
  {
    printf ("Success!\n");
    
    return 0;
  } else
  {
    elvin_perror ("Elvin", &error);
    
    return 1;
  }
}

