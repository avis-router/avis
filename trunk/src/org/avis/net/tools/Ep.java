package org.avis.net.tools;

import java.io.InputStreamReader;

import java.net.ConnectException;

import java.nio.charset.Charset;

import org.avis.common.Notification;
import org.avis.net.client.Elvin;
import org.avis.util.InvalidFormatException;

import dsto.dfc.logging.Log;

import static dsto.dfc.logging.Log.DIAGNOSTIC;
import static dsto.dfc.logging.Log.TRACE;
import static dsto.dfc.logging.Log.alarm;
import static dsto.dfc.logging.Log.setEnabled;

import static org.avis.net.client.ConnectionOptions.EMPTY_OPTIONS;
import static org.avis.net.security.Keys.EMPTY_KEYS;

/**
 * The ep command line utility. Reads a notification from standard
 * input and sends to an Elvin router.
 * 
 * @author Matthew Phillips
 */
public class Ep
{
  public static void main (String [] args)
  {
    Log.setApplicationName ("ep");

    setEnabled (TRACE, false);
    setEnabled (DIAGNOSTIC, false);
    
    EpOptions options = new EpOptions ();
    
    options.parseOrExit (args);
    
    try
    {
      final Elvin elvin =
        new Elvin (options.elvinUri, EMPTY_OPTIONS, options.keys, EMPTY_KEYS);
      
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
      
      InputStreamReader input =
        new InputStreamReader (System.in,
                               Charset.forName ("UTF-8").newDecoder ());
      
      Notification notification = new Notification (input);
      
      elvin.send (notification, options.secureMode, options.keys);
      
      System.exit (0);
    } catch (InvalidFormatException ex)
    {
      System.err.println ("ep: Invalid notification: " + ex.getMessage ());
      
      System.exit (1);
    } catch (Exception ex)
    {
      if (ex instanceof ConnectException)
        alarm ("Failed to connect to Elvin: connection refused", Ep.class);
      else
        alarm ("Error connecting to Elvin: " + ex.getMessage (), Ep.class);
      
      System.exit (1);
    }
  }
}
