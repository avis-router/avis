package org.avis.util;

/**
 * General text formatting utilities.
 * 
 * @author Matthew Phillips
 */
public final class Format
{
  private static final char [] HEX_TABLE = 
  {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
   'a', 'b', 'c', 'd', 'e', 'f'};

  private Format ()
  {
    // cannot be instantiated
  }
  
  /**
   * Return just the name (minus the package) of a class.
   */
  public static String className (Class type)
  {
    String name = type.getName ();
    
    return name.substring (name.lastIndexOf ('.') + 1);
  }

  /**
   * Generate a short exception message without package name and
   * message (if null).
   */
  public static String shortException (Throwable ex)
  {
    if (ex.getMessage () == null)
      return className (ex.getClass ());
    else
      return className (ex.getClass ()) + ": " + ex.getMessage ();
  }

  /**
   * Append a string to a builder, escaping (with '\') any instances
   * of a special character.
   */
  public static void appendEscaped (StringBuilder builder,
                                    String string, char charToEscape)
  {
    for (int i = 0; i < string.length (); i++)
    {
      char c = string.charAt (i);
      
      if (c == charToEscape)
        builder.append ('\\');
      
      builder.append (c);
    }
  }

  /**
   * Append a byte array to a builder in form: [01 e2 fe ff ...].
   */
  public static void appendBytes (StringBuilder str, byte [] bytes)
  {
    boolean first = true;
    
    for (byte b : bytes)
    {
      if (!first)
        str.append (' ');
      
      first = false;
      
      appendHex (str, b);
    }
  }

  /**
   * Append the hex form of a byte to a builder.
   */
  public static void appendHex (StringBuilder str, byte b)
  {
    str.append (HEX_TABLE [(b >>> 4) & 0x0F]);
    str.append (HEX_TABLE [(b >>> 0) & 0x0F]);
  }
}
