#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include <avis/elvin.h>

int main (int argc, const char * argv[]) 
{
  Elvin elvin;
  ElvinError error = elvin_error_create ();
  Attributes *ntfn;
  const char *uri = argc > 1 ? argv [1] : "elvin://localhost";
  
  if (!elvin_open (&elvin, uri, &error))
  {
    elvin_perror ("open", &error);
    exit (1);
  }

  ntfn = attributes_create ();
    
  attributes_set_int32 (ntfn, "favourite number", 42);
  attributes_set_string (ntfn, "some text", "paydirt");
  
  elvin_send (&elvin, ntfn, &error);

  attributes_destroy (ntfn);
  
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
