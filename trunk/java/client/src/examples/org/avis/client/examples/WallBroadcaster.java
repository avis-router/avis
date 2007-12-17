package org.avis.client.examples;

import java.io.IOException;
import java.io.InputStreamReader;

import org.avis.client.Elvin;
import org.avis.client.Notification;

import static org.avis.client.examples.ExampleOptions.USAGE;
import static org.avis.util.CommandLineOptions.handleError;

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
    try
    {
      run (new ExampleOptions (args));
    } catch (Exception ex)
    {
      handleError ("wall-broadcaster", USAGE, ex);
    }
  }
  
  private static void run (ExampleOptions options)
    throws IOException
  {
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
