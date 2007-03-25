package org.avis.net.tools;

import java.util.Date;

import java.io.IOException;

import java.net.ConnectException;
import java.net.URISyntaxException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.avis.net.client.Elvin;
import org.avis.net.client.ElvinURI;
import org.avis.net.client.NotificationEvent;
import org.avis.net.client.NotificationListener;
import org.avis.util.IllegalOptionException;

import dsto.dfc.logging.Log;

import static dsto.dfc.logging.Log.alarm;

import static org.avis.util.CommandLine.stringArg;

/**
 * The ec command line utility. Subscribes to notifications and echoes
 * them to standard output.
 * 
 * @author Matthew Phillips
 */
public class Ec
{
  private static final Object USAGE =
    "\nUsage: ec -e elvin subscription\n";

  public static void main (String [] args)
  {
    Log.setApplicationName ("ec");
    
    ElvinURI elvinUri = null;
    String subscription = null;
    
    try
    {
      for (int i = 0; i < args.length; i++)
      {
        String arg = args [i];
        
        if (arg.equals ("-e"))
        {
          elvinUri = new ElvinURI (stringArg (args, ++i));
        } else if (arg.startsWith ("-"))
        {
          throw new IllegalOptionException (arg, "Not a known option");
        } else
        {
          if (subscription == null)
            subscription = arg;
          else
            throw new IllegalOptionException ("Can only have one subscription");
        }
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
    else if (subscription == null)
      usageError ("Missing subscription");
    
    try
    {
      final Elvin elvin = new Elvin (elvinUri);
      
      System.err.println ("ec: Connected to server " +
                          elvinUri.toCanonicalString ());
      
      Runtime.getRuntime ().addShutdownHook (new Thread ()
      {
        public void run ()
        {
          System.err.println ("ec: Closing connection");
          
          elvin.close ();
        }
      });

      elvin.subscribe (subscription).addNotificationListener (new Listener ());
    } catch (IOException ex)
    {
      if (ex instanceof ConnectException)
        alarm ("Failed to connect to Elvin: connection refused", Ec.class);
      else
        alarm ("Error connecting to Elvin: " + ex.getMessage (), Ec.class);
      
      System.exit (1);
    }
  }

  private static void usageError (String message)
  {
    System.err.println ("ec: " + message);
    System.err.println (USAGE);
    System.exit (1);
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
