package org.avis.net.tools;

import java.io.IOException;
import java.io.InputStreamReader;

import java.net.ConnectException;

import java.nio.charset.Charset;

import org.avis.common.Notification;
import org.avis.net.client.Elvin;
import org.avis.util.IllegalOptionException;
import org.avis.util.InvalidFormatException;

import dsto.dfc.logging.Log;

import static dsto.dfc.logging.Log.DIAGNOSTIC;
import static dsto.dfc.logging.Log.TRACE;
import static dsto.dfc.logging.Log.alarm;
import static dsto.dfc.logging.Log.setEnabled;

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

    setEnabled (TRACE, false);
    setEnabled (DIAGNOSTIC, false);
    
    EpOptions options = new EpOptions ();
    
    try
    {
      options.parse (args);
    } catch (IllegalOptionException ex)
    {
      usageError (ex.getMessage ());
    }
    
    try
    {
      final Elvin elvin = new Elvin (options.elvinUri);
      
      System.err.println ("ep: Connected to server " +
                          options.elvinUri.toCanonicalString ());
      
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
