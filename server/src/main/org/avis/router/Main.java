package org.avis.router;

import java.util.Properties;

import java.io.IOException;

import org.apache.mina.common.ByteBuffer;

import org.avis.logging.Log;
import org.avis.util.IllegalOptionException;

import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.enableLogging;
import static org.avis.logging.Log.info;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.warn;
import static org.avis.util.CommandLine.intArg;
import static org.avis.util.CommandLine.stringArg;
import static org.avis.util.Streams.fileStream;
import static org.avis.util.Streams.propertiesFrom;
import static org.avis.util.Streams.resourceStream;

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
    
    enableLogging (TRACE, false);
    enableLogging (DIAGNOSTIC, false);
    
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
    
    RouterOptions config = new RouterOptions ();

    try
    {
      for (int i = 0; i < args.length; i++)
      {
        String arg = args [i];
      
        if (arg.equals ("-v"))
        {
          enableLogging (DIAGNOSTIC, true);
        } else if (arg.equals ("-vv"))
        {
          enableLogging (DIAGNOSTIC, true);
          enableLogging (TRACE, true);
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
      
      if (shouldLog (DIAGNOSTIC))
        ex.printStackTrace ();
      
      exit (2);
    }
    
    try
    {
      final Router router = new Router (config);
      
      Runtime.getRuntime ().addShutdownHook (new Thread ()
      {
        public void run ()
        {
          info ("Shutting down...", Main.class);
          
          router.close ();
        }
      });
      
      info ("Router listening on port " + config.get ("Port"), Main.class);
    } catch (Throwable ex)
    {
      if (ex instanceof IllegalOptionException)
        alarm ("Error in server configuration: " + ex.getMessage (), Main.class);
      else
        alarm ("Error starting router: " + ex.getMessage (), Main.class);
        
      if (shouldLog (DIAGNOSTIC))
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
}
