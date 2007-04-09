package org.avis.net.tools;

import java.io.InputStreamReader;
import java.io.Reader;

import org.avis.net.security.SecureHash;

import static org.avis.util.Text.bytesToHex;
import static org.avis.util.Text.parseHexBytes;
import static org.avis.util.Util.stringFrom;

/**
 * Command line utility to run a secure hash function on an
 * hex-encoded byte array.
 * 
 * @author Matthew Phillips
 */
public class Hash
{
  private static final Object USAGE =
    "\nUsage: hash hash\n";
  
  public static void main (String [] args)
  {
    if (args.length != 1)
      usageError ();
    
    try
    {
      SecureHash hashFunction = SecureHash.valueOf (args [0]);
      
      Reader reader = new InputStreamReader (System.in);
      
      byte [] hashedData =
        hashFunction.hash (parseHexBytes (stringFrom (reader)));
      
      System.out.println (bytesToHex (hashedData));
    } catch (Exception ex)
    {
      usageError (ex.getMessage ());
    }
  }
  
  private static void usageError ()
  {
    System.err.println (USAGE);
    System.exit (1);
  }
  
  private static void usageError (String message)
  {
    System.err.println ("hashkey: " + message);
    System.err.println (USAGE);
    System.exit (1);
  }
}
