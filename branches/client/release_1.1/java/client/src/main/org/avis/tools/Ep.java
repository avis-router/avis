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
      final Ep ep = new Ep (new EpOptions (args));
      
      Runtime.getRuntime ().addShutdownHook (new Thread ()
      {
        @Override
        public void run ()
        {
          ep.close ();
        }
      });
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

  private Elvin elvin;
  
  public Ep (EpOptions options)
    throws IOException, InvalidFormatException
  {
    this.elvin = new Elvin (options.elvinUri, options.clientOptions);

    elvin.closeOnExit ();
    
    System.err.println ("ep: Connected to server " +
                        options.elvinUri.toCanonicalString ());
    
    StreamReader input = new StreamReader (System.in);
    
    while (!input.eof () && elvin.isOpen ())
      elvin.send (new Notification (input), options.secureMode);

    elvin.close ();
  }
  
  public void close ()
  {
    elvin.close ();
  }
}
