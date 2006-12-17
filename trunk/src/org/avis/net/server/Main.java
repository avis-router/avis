package org.avis.net.server;

import java.util.Properties;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.mina.common.ByteBuffer;

import static dsto.dfc.logging.Log.DIAGNOSTIC;
import static dsto.dfc.logging.Log.TRACE;
import static dsto.dfc.logging.Log.diagnostic;
import static dsto.dfc.logging.Log.info;
import static dsto.dfc.logging.Log.isEnabled;
import static dsto.dfc.logging.Log.setEnabled;
import static dsto.dfc.logging.Log.warn;

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
    " -c file    : Load config from file\n" +
    " -pid file  : Write process ID of router to file (Unix and OS X only)\n";
  
  public static void main (String [] args)
    throws Exception
  {
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
        } else if (arg.equals ("-pid"))
        {
          createPidFile (stringArg (args, ++i));
        } else
        {
          System.out.println ();
          System.out.println (USAGE);
          System.exit (0);
        }
      }
    } catch (Exception ex)
    {
      System.err.println ();
      System.err.println ("Error starting server: " + ex.getMessage ());
      
      if (isEnabled (DIAGNOSTIC))
        ex.printStackTrace ();
      
      System.exit (1);
    }
    
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
  }

  private static void createPidFile (String pidFile)
    throws IOException, InterruptedException
  {
    if (System.getProperty  ("os.name").toLowerCase ().startsWith ("windows"))
      throw new IOException ("PID detection not supported on Windows");

    // Yuck! Someone please tell me a better way to get the PID in Java
    ProcessBuilder builder =
      new ProcessBuilder
        ("/bin/sh", "-c",
         "echo $PPID > \"" + pidFile + "\"");
    
    if (builder.start ().waitFor () != 0)
      throw new IOException ("Failed to create PID file: " + pidFile);
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

  private static int intArg (String [] args, int i)
  {
    try
    {
      return Integer.parseInt (stringArg (args, i));
    } catch (NumberFormatException ex)
    {
      throw new IllegalArgumentException
        (args [i - 1] + ": not a number: " + args [i]);
    }
  }
  
  private static String stringArg (String [] args, int i)
  {
    try
    {
      return args [i];
    } catch (ArrayIndexOutOfBoundsException ex)
    {
      throw new IllegalArgumentException
        ("Missing parameter for \"" + args [i - 1] + "\" option");
    }
  }
}
