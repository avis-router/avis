package org.avis.net.tools;

import java.io.IOException;
import java.io.InputStreamReader;

import java.net.ConnectException;
import java.net.URISyntaxException;

import java.nio.charset.Charset;

import org.avis.common.Notification;
import org.avis.net.client.Elvin;
import org.avis.net.client.ElvinURI;
import org.avis.util.IllegalOptionException;
import org.avis.util.InvalidFormatException;

import dsto.dfc.logging.Log;

import static dsto.dfc.logging.Log.alarm;

import static org.avis.util.CommandLine.stringArg;

/**
 * The ep command line utility. Reads a notification from standard
 * input and sends to en Elvin router.
 * 
 * @author Matthew Phillips
 */
public class Ep
{
  private static final Object USAGE =
    "\nUsage: ep -e elvin\n";

  public static void main (String [] args)
  {
    Log.setApplicationName ("ep");
    
    ElvinURI elvinUri = null;
    
    try
    {
      for (int i = 0; i < args.length; i++)
      {
        String arg = args [i];
        
        if (arg.equals ("-e"))
          elvinUri = new ElvinURI (stringArg (args, ++i));
        else
          throw new IllegalOptionException (arg, "Not a known option");
      }
    } catch (URISyntaxException ex)
    {
      System.err.println ("\nError in Elvin URI: " + ex.getMessage ());
      
      System.exit (1);
    } catch (IllegalOptionException ex)
    {
      usageError (ex.getMessage ());
    }
    
    if (elvinUri == null)
      usageError ("Missing Elvin URI (-e option)");
    
    try
    {
      final Elvin elvin = new Elvin (elvinUri);
      
      System.err.println ("ep: Connected to server " +
                          elvinUri.toCanonicalString ());
      
      Runtime.getRuntime ().addShutdownHook (new Thread ()
      {
        public void run ()
        {
          System.err.println ("ep: Closing connection");
          
          elvin.close ();
        }
      });
      
      Notification notification =
        new Notification
          (new InputStreamReader (System.in,
                                  Charset.forName ("UTF-8").newDecoder ()));
      
      elvin.send (notification);
      
      System.exit (0);
    } catch (IOException ex)
    {
      if (ex instanceof ConnectException)
        alarm ("Failed to connect to Elvin: connection refused", Ep.class);
      else
        alarm ("Error connecting to Elvin: " + ex.getMessage (), Ep.class);
      
      System.exit (1);
    } catch (InvalidFormatException ex)
    {
      System.err.println ("ep: Invalid notification: " + ex.getMessage ());
      
      System.exit (1);
    }
  }

  private static void usageError (String message)
  {
    System.err.println ("ep: " + message);
    System.err.println (USAGE);
    System.exit (1);
  }
}
