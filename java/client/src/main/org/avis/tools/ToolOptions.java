package org.avis.tools;

import java.util.Queue;

import java.io.IOException;

import java.net.ConnectException;

import org.avis.client.ElvinOptions;
import org.avis.client.SecureMode;
import org.avis.common.ElvinURI;
import org.avis.common.InvalidURIException;
import org.avis.security.DualKeyScheme;
import org.avis.security.Key;
import org.avis.security.Keys;
import org.avis.util.CommandLineOptions;
import org.avis.util.IllegalCommandLineOption;

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
    "-e elvin [option]...";

  protected static final String COMMON_USAGE_DETAIL =
    "  -e elvin       Set the Elvin URI e.g. elvin://host:port\n" +
    "  -x             Allow only secure notifications\n" +
    "  -X             Allow insecure notifications (default)\n" +
    "  -C file        Read consumer key from file\n" +
    "  -P file        Read producer key from file\n" +
    "  -k keystore\n" +
    "     passphrase  Set keystore and passphrase for TLS/SSL\n" +
    "  -a             Require authenticated server when using TLS\n" +
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
  
  public ElvinOptions clientOptions;
  
  /**
   * The security requirement.
   */
  public SecureMode secureMode;
  
  public ToolOptions (String... args)
  {
    this.secureMode = ALLOW_INSECURE_DELIVERY;
    this.clientOptions = new ElvinOptions ();
    
    clientOptions.notificationKeys = new Keys ();
    
    // ec/ep always use symmetrical key sets 
    clientOptions.subscriptionKeys = clientOptions.notificationKeys;
    
    handleOptions (args);
  }
  
  public static void handleIOError (String appName, IOException ex)
  {
    System.err.print (appName);
    System.err.print (": ");
    
    if (ex instanceof ConnectException)
      System.err.println ("Failed to connect to Elvin: connection refused");
    else
      System.err.println ("Error connecting to Elvin: " + ex.getMessage ());
    
    System.exit (3);
  }
  
  @Override
  protected void handleArg (Queue<String> args)
    throws IllegalCommandLineOption
  {
    String arg = args.peek ();
    
    if (arg.equals ("-e"))
    {
      try
      {
        elvinUri = new ElvinURI (stringArg (args));
      } catch (InvalidURIException ex)
      {
        throw new IllegalCommandLineOption (arg, ex.getMessage ());
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
    } else if (arg.equals ("-k"))                
    {
      loadKeystore (stringArg (args), bareArg (args));
    } else if (takeArg (args, "-a"))                
    {
      clientOptions.requireAuthenticatedServer = true;
    }
  }
  
  private void addKey (DualKeyScheme.Subset subset, Key key)
  {
    Keys keys = clientOptions.notificationKeys;
    
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
      throw new IllegalCommandLineOption
        ("Could not read key from \"" + filename + "\": " + ex.getMessage ());
    }
  }
  
  private void loadKeystore (String keystorePath, String passphrase)
    throws IllegalCommandLineOption
  {
    try
    {
      clientOptions.setKeystore (keystorePath, passphrase);
    } catch (IOException ex)
    {
      throw new IllegalCommandLineOption 
        ("-k", "Error loading key store: " + ex.getMessage ());
    }
  }

  @Override
  protected void checkOptions ()
    throws IllegalCommandLineOption
  {
    if (elvinUri == null)
      throw new IllegalCommandLineOption ("Missing Elvin URI");
    
    if (secureMode == REQUIRE_SECURE_DELIVERY && 
        clientOptions.notificationKeys.isEmpty ())
    {
      throw new IllegalCommandLineOption
        ("-x", "Cannot activate secure mode if no keys specified");
    }
  }
}
