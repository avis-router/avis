package org.avis.tools;

import org.avis.security.SecureHash;

import static org.avis.util.Text.bytesToHex;
import static org.avis.util.Text.dataToBytes;
import static org.avis.util.Util.bytesFrom;

/**
 * Command line utility to run a secure hash function on an
 * hex-encoded byte array.
 * 
 * @author Matthew Phillips
 */
public class Hash
{
  private static final Object USAGE =
    "\nUsage: hash hash_type\n";
  
  /**
   * Run hash command.
   */
  public static void main (String [] args)
  {
    if (args.length != 1)
      usageError ();
    
    try
    {
      SecureHash hashFunction = SecureHash.valueOf (args [0]);
      
      byte [] hashedData =
        hashFunction.hash (dataToBytes (bytesFrom (System.in)));
      
      System.out.print ('[');
      System.out.print (bytesToHex (hashedData));
      System.out.print (']');
      System.out.println ();
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
