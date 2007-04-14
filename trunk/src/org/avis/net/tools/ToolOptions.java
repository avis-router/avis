package org.avis.net.tools;

import java.util.Queue;

import java.net.URISyntaxException;

import org.avis.net.common.ElvinURI;
import org.avis.net.security.Key;
import org.avis.net.security.Keys;
import org.avis.util.CommandLineOptions;
import org.avis.util.IllegalOptionException;

import static org.avis.net.security.DualKeyScheme.CONSUMER;
import static org.avis.net.security.DualKeyScheme.PRODUCER;
import static org.avis.net.security.KeyScheme.SHA1_CONSUMER;
import static org.avis.net.security.KeyScheme.SHA1_DUAL;
import static org.avis.net.security.KeyScheme.SHA1_PRODUCER;
import static org.avis.util.Text.hexToBytes;
import static org.avis.util.Util.fileStream;
import static org.avis.util.Util.stringFrom;

/**
 * Command line options that are common across ec and ep.
 * 
 * @author Matthew Phillips
 */
public abstract class ToolOptions extends CommandLineOptions
{
  protected static final String COMMON_USAGE_SUMMARY =
    "-e elvin [-x] [-X] [-C file] [-P file]";

  protected static final String COMMON_USAGE_DETAIL =
    "  -e elvin  Set the Elvin URI e.g. elvin://host:port\n" +
    "  -x        Allow only secure notifications\n" +
    "  -X        Allow insecure notifications (default)\n" +
    "  -C file   Read hex-coded consumer key from file\n" +
    "  -P file   Read hex-coded producer key from file";
  
  public ElvinURI elvinUri;
  public Keys keys;
  public boolean insecure;
  
  public ToolOptions ()
  {
    this.insecure = true;
    this.keys = new Keys ();
  }
  
  @Override
  protected void handleArg (Queue<String> args)
    throws IllegalOptionException
  {
    String arg = args.peek ();
    
    if (arg.equals ("-e"))
    {
      try
      {
        elvinUri = new ElvinURI (stringArg (args));
      } catch (URISyntaxException ex)
      {
        throw new IllegalOptionException
          (arg, "Invalid Elvin URI: " + ex.getMessage ());
      }
    } else if (takeArg (args, "-x"))
    {
      insecure = false;
    } else if (takeArg (args, "-X"))
    {
      insecure = true;
    } else if (arg.equals ("-C"))
    {
      addKeys (CONSUMER, readHexKeyFrom (stringArg (args)));
    } else if (arg.equals ("-P"))                
    {
      addKeys (PRODUCER, readHexKeyFrom (stringArg (args)));
    }
  }
  
  private void addKeys (int type, Key key)
  {
    keys.add (type == CONSUMER ? SHA1_CONSUMER : SHA1_PRODUCER, key);
    keys.add (SHA1_DUAL, type, key);
  }

  private static Key readHexKeyFrom (String filename)
  {
    try
    {
      return new Key (hexToBytes (stringFrom (fileStream (filename))));
    } catch (Exception ex)
    {
      throw new IllegalOptionException
        ("Could not read key from \"" + filename + "\": " + ex.getMessage ());
    }
  }

  @Override
  protected void checkOptions ()
    throws IllegalOptionException
  {
    if (elvinUri == null)
      throw new IllegalOptionException ("Missing Elvin URI");
    
    if (!insecure && keys.isEmpty ())
      throw new IllegalOptionException
        ("-x", "Cannot activate secure mode if no keys specified");
  }
}
