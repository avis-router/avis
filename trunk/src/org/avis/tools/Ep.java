package org.avis.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;

import java.net.ConnectException;

import org.avis.client.Elvin;
import org.avis.client.Notification;
import org.avis.util.InvalidFormatException;

import dsto.dfc.logging.Log;

import static dsto.dfc.logging.Log.DIAGNOSTIC;
import static dsto.dfc.logging.Log.TRACE;
import static dsto.dfc.logging.Log.alarm;
import static dsto.dfc.logging.Log.setEnabled;

import static org.avis.client.ConnectionOptions.EMPTY_OPTIONS;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.util.Util.eof;

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
      
      Reader input =
        new BufferedReader (new InputStreamReader (System.in, "UTF-8"));
      
      while (!eof (input))
      {
        elvin.send (new Notification (input), options.secureMode, options.keys);
      }
      
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
