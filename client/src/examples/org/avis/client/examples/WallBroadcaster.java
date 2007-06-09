package org.avis.client.examples;

import java.io.InputStreamReader;

import org.avis.client.Elvin;
import org.avis.client.Notification;

/*
 * This is the broadcaster part of the "wall" example. In this
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
public class WallBroadcaster
{
  public static void main (String [] args)
    throws Exception
  {
    // read command line options
    ExampleOptions options = new ExampleOptions ("wall-broadcaster");
    
    options.parseOrExit (args);
    
    Elvin elvin = new Elvin (options.elvinUri);
    
    elvin.closeOnExit ();
    
    System.out.println ("Type some lines of text to be broadcast over Elvin.");
    
    if (System.getProperty ("os.name").startsWith ("Windows"))
      System.out.println ("Hit Ctrl+Z to exit.");
    else
      System.out.println ("Hit Ctrl+D to exit.");
    
    // read from stdin one character at a time and send over Elvin
    InputStreamReader in = new InputStreamReader (System.in);
    
    int ch;
    
    while ((ch = in.read ()) != -1)
    {
      Notification notification = new Notification ();
      
      notification.set ("From", "wall");
      notification.set ("Typed-Character", ch);
      
      elvin.send (notification);
    }
    
    elvin.close ();
  }
}
