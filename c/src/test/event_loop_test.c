#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include <elvin/elvin.h>
#include <elvin/named_values.h>

void sub_listener (Subscription *sub, Notification *notification);

int main (int argc, const char * argv[]) 
{
  Elvin elvin;
  Subscription sub;
  ElvinError error = elvin_error_create ();
  const char *uri = argc > 1 ? argv [1] : "elvin://localhost";
  
  if (!elvin_open (&elvin, uri, &error))
  {
    elvin_perror ("open", &error);
    exit (1);
  }
  
  /* TODO handle Ctrl+C */
  
  elvin_subscription_init (&sub, "require (test) && string (message)");
  
  if (!elvin_subscribe (&elvin, &sub, &error))
  {
    elvin_perror ("subscribe", &error);
    exit (1);
  }
  
  elvin_subscription_add_listener (&sub, sub_listener);
  
  while (elvin_is_open (&elvin) && elvin_error_ok (&error))
  {
    elvin_poll (&elvin, &error);
  }
  
  if (elvin_error_occurred (&error))
    elvin_perror ("receive", &error);
  
  elvin_close (&elvin);
  
  return 0;
}

void sub_listener (Subscription *sub, Notification *notification)
{
  printf ("Notified! Message = %s\n", 
          named_values_get_string (&notification->attributes, "message"));
}