package org.avis.client.examples;

import java.io.InputStreamReader;

import org.avis.client.Elvin;
import org.avis.client.Notification;

/*
 * This is the broadcaster part of the "wall" example. In this
 * example, one or more "wall" broadcasters send typed characters from
 * the console to zero or more wall receivers.
 * 
 * See the documentation in "doc/examples.txt" for instructions on how to
 * use the Avis examples.
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
    System.out.println ("Hit Ctrl+C to exit.");
    
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
