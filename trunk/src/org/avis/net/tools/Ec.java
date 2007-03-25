package org.avis.net.tools;

import java.util.Date;

import java.io.IOException;

import java.net.ConnectException;
import java.net.URISyntaxException;

import java.text.SimpleDateFormat;

import org.avis.net.client.Elvin;
import org.avis.net.client.ElvinURI;
import org.avis.net.client.NotificationEvent;
import org.avis.net.client.NotificationListener;
import org.avis.net.client.Subscription;
import org.avis.util.IllegalOptionException;

import dsto.dfc.logging.Log;

import static dsto.dfc.logging.Log.alarm;

import static org.avis.util.CommandLine.stringArg;

public class Ec
{
  private static final Object USAGE =
    "\nUsage: ec -e elvin subscription\n\n";

  public static void main (String [] args)
  {
    Log.setApplicationName ("ec");
    
    ElvinURI elvinUri = null;
    String subscriptionExpr = null;
    
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
          if (subscriptionExpr == null)
            subscriptionExpr = arg;
          else
            throw new IllegalOptionException ("Can only have one subscription");
        }
      }
    } catch (URISyntaxException ex)
    {
      System.err.println ("\nError in Elvin URI: " + ex.getMessage ());
      
      System.exit (1);
    }
    
    if (elvinUri == null)
      usageError ("Missing Elvin URI (-e option)");
    else if (subscriptionExpr == null)
      usageError ("Missing subscription");
    
    try
    {
      final Elvin elvin = new Elvin (elvinUri);
      
      Subscription subscription = elvin.subscribe (subscriptionExpr);
      
      subscription.addNotificationListener (new Listener ());
      
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
    } catch (IOException ex)
    {
      if (ex instanceof ConnectException)
        alarm ("Failed to connect to Elvin: connection refused", Ec.class);
      else
        alarm ("Error connecting to Elvin", Ec.class, ex);
    }
  }

  private static void usageError (String message)
  {
    System.err.println (message);
    System.err.println (USAGE);
    System.exit (1);
  }
  
  static class Listener implements NotificationListener
  {
    private SimpleDateFormat dateFormat;

    public Listener ()
    {
      dateFormat = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss'.'SZ");
    }
    
    public void notificationReceived (NotificationEvent e)
    {
      System.out.println ("$time " + dateFormat.format (new Date ()));
      System.out.println (e.notification);
      System.out.println ("---");
    }
  }
}
