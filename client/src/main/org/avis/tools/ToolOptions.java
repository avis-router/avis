package org.avis.tools;

import java.util.Queue;

import org.avis.client.SecureMode;
import org.avis.common.ElvinURI;
import org.avis.common.InvalidURIException;
import org.avis.security.DualKeyScheme;
import org.avis.security.Key;
import org.avis.security.Keys;
import org.avis.util.CommandLineOptions;
import org.avis.util.IllegalOptionException;

import static org.avis.client.SecureMode.ALLOW_INSECURE_DELIVERY;
import static org.avis.client.SecureMode.REQUIRE_SECURE_DELIVERY;
import static org.avis.security.DualKeyScheme.Subset.CONSUMER;
import static org.avis.security.DualKeyScheme.Subset.PRODUCER;
import static org.avis.security.KeyScheme.SHA1_CONSUMER;
import static org.avis.security.KeyScheme.SHA1_DUAL;
import static org.avis.security.KeyScheme.SHA1_PRODUCER;
import static org.avis.util.Streams.bytesFrom;
import static org.avis.util.Streams.fileStream;
import static org.avis.util.Text.dataToBytes;

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
    "  -C file   Read consumer key from file\n" +
    "  -P file   Read producer key from file\n" +
    "\n" +
    "  Key file formats are: \n" +
    "\n" +
    "     Hex coded  e.g. [00 de ad be ef]\n" +
    "     String     e.g. \"some characters\"\n" +
    "     Raw data   e.g. #<any data>\n";
  
  /**
   * The Elvin router to connect to.
   */
  public ElvinURI elvinUri;
  
  /**
   * The security keys.
   */
  public Keys keys;
  
  /**
   * The security requirement.
   */
  public SecureMode secureMode;
  
  ToolOptions ()
  {
    this.secureMode = ALLOW_INSECURE_DELIVERY;
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
      } catch (InvalidURIException ex)
      {
        throw new IllegalOptionException
          (arg, "Invalid Elvin URI: " + ex.getMessage ());
      }
    } else if (takeArg (args, "-x"))
    {
      secureMode = REQUIRE_SECURE_DELIVERY;
    } else if (takeArg (args, "-X"))
    {
      secureMode = ALLOW_INSECURE_DELIVERY;
    } else if (arg.equals ("-C"))
    {
      addKey (CONSUMER, keyFromFile (stringArg (args)));
    } else if (arg.equals ("-P"))                
    {
      addKey (PRODUCER, keyFromFile (stringArg (args)));
    }
  }
  
  private void addKey (DualKeyScheme.Subset subset, Key key)
  {
    keys.add (subset == CONSUMER ? SHA1_CONSUMER : SHA1_PRODUCER, key);
    keys.add (SHA1_DUAL, subset, key);
  }

  private static Key keyFromFile (String filename)
  {
    try
    {
      return new Key (dataToBytes (bytesFrom (fileStream (filename))));
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
    
    if (secureMode == REQUIRE_SECURE_DELIVERY && keys.isEmpty ())
      throw new IllegalOptionException
        ("-x", "Cannot activate secure mode if no keys specified");
  }
}
