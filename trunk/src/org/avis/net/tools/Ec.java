package org.avis.net.tools;

import java.io.IOException;

import java.net.ConnectException;
import java.net.URISyntaxException;

import org.avis.net.client.Elvin;
import org.avis.net.client.ElvinURI;
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
      final Elvin elvin = new Elvin (elvinUri);
      
      info ("Connected to " + elvinUri.toCanonicalString (), Ec.class);
      
      Runtime.getRuntime ().addShutdownHook (new Thread ()
      {
        public void run ()
        {
          info ("Shutting down...", Main.class);
          
          elvin.close ();
        }
      });
    } catch (IOException ex)
    {
      if (ex instanceof ConnectException)
        Log.alarm ("Failed to connect to Elvin: connection refused", Ec.class);
      else
        Log.alarm ("Error connecting to Elvin", Ec.class, ex);
    }
  }
}
