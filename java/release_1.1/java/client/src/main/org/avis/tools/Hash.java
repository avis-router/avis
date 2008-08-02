package org.avis.tools;

import org.avis.security.SecureHash;

import static org.avis.util.Streams.bytesFrom;
import static org.avis.util.Text.bytesToHex;
import static org.avis.util.Text.dataToBytes;
import static org.avis.util.Text.join;

/**
 * Command line utility to run a secure hash function on an
 * hex-encoded byte array.
 * 
 * @author Matthew Phillips
 */
public class Hash
{
  private static final Object USAGE =
    "\nUsage: hash hash_function\n" +
    "\n" +
    "  Data is read from standard input, hashed data is written to\n" +
    "  standard output.\n" +
    "\n" +
    "  Supported hash functions:\n" +
    "      " + join (SecureHash.values ()) + "\n";
  
  /**
   * Run hash command.
   */
  public static void main (String [] args)
  {
    if (args.length != 1 || args [0].equals ("-h"))
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
    System.err.println ("hash: " + message);
    System.err.println (USAGE);
    System.exit (1);
  }
}
