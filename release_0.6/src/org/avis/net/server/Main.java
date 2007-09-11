package org.avis.net.server;

import java.util.Properties;

import java.io.IOException;
import java.io.InputStream;

import static dsto.dfc.logging.Log.DIAGNOSTIC;
import static dsto.dfc.logging.Log.TRACE;
import static dsto.dfc.logging.Log.alarm;
import static dsto.dfc.logging.Log.info;
import static dsto.dfc.logging.Log.setEnabled;
import static dsto.dfc.logging.Log.warn;

/**
 * Run the router from the command line.
 * 
 * @author Matthew Phillips
 */
public class Main
{
  private static final String USAGE =
    "Usage: avisd [-h] [-v] [-vv] [-p port]\n\n" +
    " -h This text\n" +
    " -v and -vv increase verbosity\n" +
    " -p port set port to listen on\n";
  
  public static void main (String [] args)
    throws Exception
  {
    setEnabled (TRACE, false);
    setEnabled (DIAGNOSTIC, false);
    
    Properties properties = loadProperties ();
    System.getProperties ().putAll (properties);
    
    info ("Avis router version " +
          properties.getProperty ("avis.router.version"), Main.class);
    
    int port = 2917;

    try
    {
      for (int i = 0; i < args.length; i++)
      {
        String arg = args [i];
      
        if (arg.equals ("-v"))
        {
          setEnabled (DIAGNOSTIC, true);
        } else if (arg.equals ("-vv"))
        {
          setEnabled (DIAGNOSTIC, true);
          setEnabled (TRACE, true);
        } else if (arg.equals ("-p"))
        {
          port = intArg (args, ++i);
        } else
        {
          System.out.println (USAGE);
          System.exit (0);
        }
      }
    } catch (IllegalArgumentException ex)
    {
      System.out.println (ex.getMessage ());
      System.exit (1);
    }

    final Server server = new Server (port);
    
    Runtime.getRuntime ().addShutdownHook (new Thread ()
    {
      @Override
      public void run ()
      {
        info ("Shutting down...", Main.class);
        
        server.close ();
      }
    });
    
    info ("Server listening on port " + port, Main.class);
  }

  private static Properties loadProperties ()
  {
    Properties properties = new Properties ();
    
    InputStream in = Main.class.getResourceAsStream ("/avis.properties");
    
    if (in != null)
    {  
      try
      {
        properties.load (in);
      } catch (IOException ex)
      {
        alarm ("Error loading Avis property file", Main.class, ex);
      } finally
      {
        try
        {
          in.close ();
        } catch (IOException ex)
        {
          // zip
        }
      } 
    } else
    {
      warn ("Failed to find Avis property file", Main.class);
    }
   
    if (!properties.containsKey ("avis.router.version"))
       properties.put ("avis.router.version", "<unknown>");
    
    return properties;
  }

  private static int intArg (String [] args, int i)
  {
    try
    {
      return Integer.parseInt (args [i]);
    } catch (ArrayIndexOutOfBoundsException ex)
    {
      throw new IllegalArgumentException
        ("Missing parameter for \"" + args [i - 1] + "\" option");
    } catch (NumberFormatException ex)
    {
      throw new IllegalArgumentException
        (args [i - 1] + ": not a number: " + args [i]);
    }
  }
}
