package org.avis.net.tools;

import java.net.ConnectException;
import java.net.URISyntaxException;

import org.apache.mina.common.RuntimeIOException;

import org.avis.net.client.ElvinURI;
import org.avis.net.client.Router;
import org.avis.net.server.Main;

import dsto.dfc.logging.Log;

import static dsto.dfc.logging.Log.info;

import static org.avis.util.CommandLine.stringArg;

public class Ec
{
  private static final Object USAGE =
    "\nUsage: ec -e elvin subscription\n\n";

  public static void main (String [] args)
  {
    ElvinURI elvinUri = null;
    
    try
    {
      for (int i = 0; i < args.length; i++)
      {
        String arg = args [i];
        
        if (arg.equals ("-e"))
        {
          elvinUri = new ElvinURI (stringArg (args, ++i));
        }
      }
    } catch (URISyntaxException ex)
    {
      System.err.println ("\nError in Elvin URI: " + ex.getMessage ());
      
      System.exit (1);
    }
    
    if (elvinUri == null)
    {
      System.err.println ("Missing Elvin URI (-e option)");
      System.err.println (USAGE);
      System.exit (1);
    }
    
    try
    {
      final Router router = new Router (elvinUri);
      
      info ("Connected to " + elvinUri.toCanonicalString (), Ec.class);
      
      Runtime.getRuntime ().addShutdownHook (new Thread ()
      {
        public void run ()
        {
          info ("Shutting down...", Main.class);
          
          router.close ();
        }
      });
    } catch (RuntimeIOException ex)
    {
      if (ex.getCause () instanceof ConnectException)
        Log.alarm ("Failed to connect to Elvin: connection refused", Ec.class);
      else
        Log.alarm ("Error connecting to Elvin", Ec.class, ex);
    }
  }
}
