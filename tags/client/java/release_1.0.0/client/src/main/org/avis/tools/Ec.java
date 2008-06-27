package org.avis.tools;

import java.util.Date;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.net.ConnectException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.avis.client.CloseEvent;
import org.avis.client.CloseListener;
import org.avis.client.Elvin;
import org.avis.client.GeneralNotificationEvent;
import org.avis.client.GeneralNotificationListener;

import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.setApplicationName;
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
    setApplicationName ("ec");

    EcOptions options = new EcOptions ();

    options.parseOrExit (args);
    
    try
    {
      final Elvin elvin =
        new Elvin (options.elvinUri, EMPTY_KEYS, options.keys);
      
      System.err.println ("ec: Connected to server " +
                          options.elvinUri.toCanonicalString ());
      
      elvin.addCloseListener (new CloseListener ()
      {
        public void connectionClosed (CloseEvent e)
        {
          System.err.println ("ec: Connection closed: " + e.message);
        }
      });
      
      Runtime.getRuntime ().addShutdownHook (new Thread ()
      {
        @Override
        public void run ()
        {
          elvin.close ();
        }
      });

      elvin.subscribe (options.subscription, options.keys, options.secureMode);
      
      elvin.addNotificationListener (new Listener ());
      
    } catch (Exception ex)
    {
      if (ex instanceof ConnectException)
        alarm ("Failed to connect to Elvin: connection refused", Ec.class);
      else
        alarm ("Error connecting to Elvin: " + ex.getMessage (), Ec.class);
      
      System.exit (1);
    }
  }

  static class Listener implements GeneralNotificationListener
  {
    private DateFormat iso8601Date;
    private BufferedWriter output;

    Listener () 
      throws IOException
    {
      iso8601Date = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
      output =
        new BufferedWriter (new OutputStreamWriter (System.out, "UTF-8"));
    }
    
    public void notificationReceived (GeneralNotificationEvent e)
    {
      try
      {
        print ("$time " + iso8601Date.format (new Date ()));
        
        if (!e.secureMatches ().isEmpty ())
          println (" [secure]");
        else
          println ();
        
        println (e.notification.toString ());
        println ("---");
        
        output.flush ();
      } catch (IOException ex)
      {
        ex.printStackTrace ();
      }
    }

    private void print (String string)
      throws IOException
    {
      output.write (string);
    }
    
    private void println (String string)
      throws IOException
    {
      print (string);
      println ();
    }

    private void println ()
      throws IOException
    {
      output.write ('\n');
    }
  }
}
