package org.avis.net.tools;

import java.util.Date;

import java.net.ConnectException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.avis.net.client.Elvin;
import org.avis.net.client.NotificationEvent;
import org.avis.net.client.NotificationListener;
import org.avis.net.client.Subscription;

import dsto.dfc.logging.Log;

import static dsto.dfc.logging.Log.DIAGNOSTIC;
import static dsto.dfc.logging.Log.TRACE;
import static dsto.dfc.logging.Log.alarm;
import static dsto.dfc.logging.Log.setEnabled;

import static org.avis.net.client.ConnectionOptions.EMPTY_OPTIONS;
import static org.avis.net.security.Keys.EMPTY_KEYS;

/**
 * The ec command line utility. Subscribes to notifications and echoes
 * them to standard output.
 * 
 * @author Matthew Phillips
 */
public class Ec
{
  public static void main (String [] args)
  {
    Log.setApplicationName ("ec");

    setEnabled (TRACE, false);
    setEnabled (DIAGNOSTIC, false);
    
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
        public void run ()
        {
          System.err.println ("ec: Closing connection");
          
          elvin.close ();
        }
      });

      Subscription sub;
      
      if (options.insecure)
        sub = elvin.subscribe (options.subscription);
      else
        sub = elvin.subscribeSecure (options.subscription);
      
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
    private DateFormat iso8601Date = 
      new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public void notificationReceived (NotificationEvent e)
    {
      System.out.println ("$time " + iso8601Date.format (new Date ()));
      System.out.println (e.notification);
      System.out.println ("---");
    }
  }
}
