package org.avis.net.server;

import java.util.Properties;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.mina.common.ByteBuffer;

import org.avis.util.IllegalOptionException;

import dsto.dfc.logging.Log;

import static dsto.dfc.logging.Log.DIAGNOSTIC;
import static dsto.dfc.logging.Log.TRACE;
import static dsto.dfc.logging.Log.alarm;
import static dsto.dfc.logging.Log.diagnostic;
import static dsto.dfc.logging.Log.info;
import static dsto.dfc.logging.Log.isEnabled;
import static dsto.dfc.logging.Log.setEnabled;
import static dsto.dfc.logging.Log.warn;

import static org.avis.util.CommandLine.intArg;
import static org.avis.util.CommandLine.stringArg;

/**
 * Invokes the Avis router from the command line.
 * 
 * @author Matthew Phillips
 */
public class Main
{
  private static final String USAGE =
    "Usage: avisd [-h] [-v] [-vv] [-p port] [-c file]\n\n" +
    " -h         : This text\n" +
    " -v and -vv : Increase verbosity\n" +
    " -p port    : Set port to listen on\n" +
    " -c file    : Load config from file\n";
  
  public static void main (String [] args)
    throws Exception
  {
    Log.setApplicationName ("Avis");
    
    setEnabled (TRACE, false);
    setEnabled (DIAGNOSTIC, false);
    
    /*
     * todo opt: consider heap or direct buffer setting. Some
     * anecdotal evidence on the MINA mailing list indicates heap
     * buffers are faster than direct ones, but basic multi-client
     * profiling seems to indicate heap buffers are actually slightly
     * slower, at least on a G4 with 1.5.0_06.
     */
    ByteBuffer.setUseDirectBuffers (true);
    
    Properties avisProperties = readAvisProperties ();
    System.getProperties ().putAll (avisProperties);
    
    info ("Avis event router version " +
          avisProperties.getProperty ("avis.router.version"), Main.class);
    
    ServerOptions config = new ServerOptions ();

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
          config.set ("Port", intArg (args, ++i));
        } else if (arg.equals ("-c"))
        {
          String configFile = stringArg (args, ++i);
          
          config.setAll (propertiesFrom (fileStream (configFile)));
          
          diagnostic ("Read configuration from " + configFile, Main.class);
        } else
        {
          System.err.println ("\nUnknown option: \"" + arg + "\"\n");
          System.err.println (USAGE);
          System.exit (1);
        }
      }
    } catch (Exception ex)
    {
      alarm ("Error configuring server: " + ex.getMessage (), Main.class);
      
      if (isEnabled (DIAGNOSTIC))
        ex.printStackTrace ();
      
      exit (2);
    }
    
    try
    {
      final Server server = new Server (config);
      
      Runtime.getRuntime ().addShutdownHook (new Thread ()
      {
        public void run ()
        {
          info ("Shutting down...", Main.class);
          
          server.close ();
        }
      });
      
      info ("Server listening on port " + config.get ("Port"), Main.class);
    } catch (Throwable ex)
    {
      if (ex instanceof IllegalOptionException)
        alarm ("Error in server configuration: " + ex.getMessage (), Main.class);
      else
        alarm ("\nError starting server: " + ex.getMessage (), Main.class);
        
      if (isEnabled (DIAGNOSTIC))
        ex.printStackTrace ();
      
      exit (2);
    }
  }

  private static void exit (int errorCode)
  {
    info ("Exiting on error", Main.class);
    System.exit (errorCode);
  }

  private static Properties readAvisProperties ()
    throws IOException
  {
    Properties properties;
    
    try
    {
      properties = propertiesFrom (resourceStream ("/avis.properties"));
    } catch (IOException ex)
    {
      warn ("Failed to load Avis property file: " + ex.getMessage (),
            Main.class);
      
      properties = new Properties ();
    }
    
    if (!properties.containsKey ("avis.router.version"))
      properties.put ("avis.router.version", "<unknown>");
    
    return properties;
  }

  private static InputStream fileStream (String filename)
    throws FileNotFoundException
  {
    return new BufferedInputStream (new FileInputStream (filename));
  }

  private static InputStream resourceStream (String resource)
    throws FileNotFoundException
  {
    InputStream in = Main.class.getResourceAsStream (resource);
    
    if (in == null)
      throw new FileNotFoundException ("Missing resource: " + resource);
    else
      return in;
  }
  
  private static Properties propertiesFrom (InputStream in)
    throws IOException
  {
    Properties properties = new Properties ();
    
    try
    {
      properties.load (in);
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
   
    return properties;
  }
}
