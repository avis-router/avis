package org.avis.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.avis.client.Elvin;
import org.avis.client.Notification;
import org.avis.util.InvalidFormatException;

import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.tools.EpOptions.USAGE;
import static org.avis.tools.ToolOptions.handleIOError;
import static org.avis.util.CommandLineOptions.handleError;
import static org.avis.util.Streams.eof;

/**
 * The ep command line utility. Reads a notification from standard
 * input and sends to an Elvin router.
 * 
 * @author Matthew Phillips
 */
public class Ep
{
  /**
   * Run ep.
   */
  public static void main (String [] args)
    throws Exception
  {
    try
    {
      run (new EpOptions (args));
    } catch (InvalidFormatException ex)
    {
      System.err.println ("ep: Invalid notification: " + ex.getMessage ());
      
      System.exit (4);
    } catch (IOException ex)
    {
      handleIOError ("ec", ex);
    } catch (Exception ex)
    {
      handleError ("ec", USAGE, ex);
    }
  }
  
  private static void run (EpOptions options)
    throws IOException, InvalidFormatException
  {
    final Elvin elvin =
      new Elvin (options.elvinUri, options.keys, EMPTY_KEYS);
    
    System.err.println ("ep: Connected to server " +
                        options.elvinUri.toCanonicalString ());
    
    Runtime.getRuntime ().addShutdownHook (new Thread ()
    {
      @Override
      public void run ()
      {
        System.err.println ("ep: Closing connection");
        
        elvin.close ();
      }
    });
    
    Reader input =
      new BufferedReader (new InputStreamReader (System.in, "UTF-8"));
    
    while (!eof (input))
      elvin.send (new Notification (input), options.secureMode);
  }
}
