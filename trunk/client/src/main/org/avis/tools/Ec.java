package org.avis.tools;

import java.util.Date;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.net.ConnectException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.avis.client.Elvin;
import org.avis.client.NotificationEvent;
import org.avis.client.NotificationListener;
import org.avis.client.Subscription;

import org.avis.logging.Log;

import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.enableLogging;

import static org.avis.client.ConnectionOptions.EMPTY_OPTIONS;
import static org.avis.security.Keys.EMPTY_KEYS;

/**
 * The ec command line utility. Subscribes to notifications and echoes
 * them to standard output.
 * 
 * @author Matthew Phillips
 */
public class Ec
{
  /**
   * Run ec.
   */
  public static void main (String [] args)
  {
    Log.setApplicationName ("ec");

    enableLogging (TRACE, false);
    enableLogging (DIAGNOSTIC, false);
    
    EcOptions options = new EcOptions ();

    options.parseOrExit (args);
    
    try
    {
      final Elvin elvin =
        new Elvin (options.elvinUri, EMPTY_OPTIONS, EMPTY_KEYS, options.keys);
      
      System.err.println ("ec: Connected to server " +
                          options.elvinUri.toCanonicalString ());
      
      Runtime.getRuntime ().addShutdownHook (new Thread ()
      {
        @Override
        public void run ()
        {
          System.err.println ("ec: Closing connection");
          
          elvin.close ();
        }
      });

      Subscription sub =
        elvin.subscribe (options.subscription, options.secureMode,
                         options.keys);
      
      sub.addNotificationListener (new Listener ());
    } catch (Exception ex)
    {
      if (ex instanceof ConnectException)
        alarm ("Failed to connect to Elvin: connection refused", Ec.class);
      else
        alarm ("Error connecting to Elvin: " + ex.getMessage (), Ec.class);
      
      System.exit (1);
    }
  }

  static class Listener implements NotificationListener
  {
    private DateFormat iso8601Date;
    private BufferedWriter output;

    Listener () 
      throws IOException
    {
      iso8601Date = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
      output = new BufferedWriter (new OutputStreamWriter (System.out, "UTF-8"));
    }
    
    public void notificationReceived (NotificationEvent e)
    {
      try
      {
        println ("$time " + iso8601Date.format (new Date ()));
        println (e.notification.toString ());
        println ("---");
        
        output.flush ();
      } catch (IOException ex)
      {
        ex.printStackTrace ();
      }
    }

    private void println (String string)
      throws IOException
    {
      output.write (string);
      output.write ('\n');
    }
  }
}
