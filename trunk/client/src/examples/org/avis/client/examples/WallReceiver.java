package org.avis.client.examples;

import org.avis.client.Elvin;
import org.avis.client.GeneralNotificationEvent;
import org.avis.client.GeneralNotificationListener;

/*
 * This is the receiver part of the "wall" example. In this
 * example, one or more "wall" broadcasters send typed characters from
 * the console to zero or more wall receivers.
 * 
 * Running The Example
 * 
 * You will need an Elvin router -- either install the Avis router (or
 * other compatible router) locally, or use the public Elvin router at
 * public.elvin.org.
 * 
 * To run a receiver from the console:
 * 
 * > ant example-wall-receiver
 * 
 * To run the broadcaster, open a new console and run:
 * 
 * > ant example-wall-broadcaster
 * 
 * Try running multiple broadcasters and receivers.
 * 
 * If you aren't using a local router (elvin://localhost), add
 * -Delvin=elvin://public.elvin.org to the commands, e.g.
 * 
 * > ant example-wall-broadcaster -Delvin=elvin://public.elvin.org
 */
public class WallReceiver
{
  public static void main (String [] args)
    throws Exception
  {
    //  read command line options
    ExampleOptions options = new ExampleOptions ("wall-broadcaster");
    
    options.parseOrExit (args);
    
    Elvin elvin = new Elvin (options.elvinUri);
    
    elvin.closeOnExit ();
    
    // receive wall messages with an integer Typed-Character field
    elvin.subscribe ("From == 'wall' && int32 (Typed-Character)");
    
    elvin.addNotificationListener (new GeneralNotificationListener ()
    {
      public void notificationReceived (GeneralNotificationEvent e)
      {
        System.out.print ((char)e.notification.getInt ("Typed-Character"));
      }
    });
    
    System.out.println ("Listening for Wall messages...");
    System.out.println ("Hit Ctrl+C to exit.");
  }
}
