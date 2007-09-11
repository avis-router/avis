package org.avis.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;

import java.net.ConnectException;

import org.avis.client.Elvin;
import org.avis.client.Notification;
import org.avis.util.InvalidFormatException;

import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.setApplicationName;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.util.Streams.eof;

/**
 * The ep command line utility. Reads a notification from standard
 * input and sends to an Elvin router.
 * 
 * @author Matthew Phillips
 */
public class Ep
{
  /**
   * Run ep.
   */
  public static void main (String [] args)
  {
    setApplicationName ("ep");

    EpOptions options = new EpOptions ();
    
    options.parseOrExit (args);
    
    try
    {
      final Elvin elvin =
        new Elvin (options.elvinUri, options.keys, EMPTY_KEYS);
      
      System.err.println ("ep: Connected to server " +
                          options.elvinUri.toCanonicalString ());
      
      Runtime.getRuntime ().addShutdownHook (new Thread ()
      {
        @Override
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
        elvin.send (new Notification (input), options.secureMode);
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
