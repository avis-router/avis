package org.avis.tools;

import java.io.IOException;

import org.avis.client.Elvin;
import org.avis.client.Notification;
import org.avis.io.StreamReader;
import org.avis.util.InvalidFormatException;

import static org.avis.tools.EpOptions.USAGE;
import static org.avis.tools.ToolOptions.handleIOError;
import static org.avis.util.CommandLineOptions.handleError;

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
      new Ep (new EpOptions (args));
    } catch (InvalidFormatException ex)
    {
      System.err.println ("ep: Invalid notification: " + ex.getMessage ());
      
      System.exit (4);
    } catch (IOException ex)
    {
      handleIOError ("ep", ex);
    } catch (Exception ex)
    {
      handleError ("ep", USAGE, ex);
    }
  }

  public Ep (EpOptions options)
    throws IOException, InvalidFormatException
  {
    Elvin elvin = new Elvin (options.elvinUri, options.clientOptions);

    try
    {
      elvin.closeOnExit ();
    
      System.err.println ("ep: Connected to server " +
                          options.elvinUri.toCanonicalString ());
    
      StreamReader input = new StreamReader (System.in);
    
      while (!input.eof () && elvin.isOpen ())
        elvin.send (new Notification (input), options.secureMode);
    } finally
    {
      elvin.close ();
    }
  }  
}
