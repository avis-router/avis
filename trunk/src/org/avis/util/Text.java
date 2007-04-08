package org.avis.util;

import java.util.Map;


/**
 * General text formatting utilities.
 * 
 * @author Matthew Phillips
 */
public final class Text
{
  private static final char [] HEX_TABLE = 
    {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
     'a', 'b', 'c', 'd', 'e', 'f'};
  
  private static final String [] EMPTY_STRING_ARRAY = new String [0];

  private Text ()
  {
    // cannot be instantiated
  }
  
  /**
   * Return just the name (minus the package) of an object's class.
   */
  public static String className (Object object)
  { 
    return className (object.getClass ());
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
   * Append a string to a builder, escaping (with '\') any instances
   * of a set of special characters.
   */
  public static void appendEscaped (StringBuilder builder,
                                    String string, String charsToEscape)
  {
    for (int i = 0; i < string.length (); i++)
    {
      char c = string.charAt (i);
      
      if (charsToEscape.indexOf (c) != -1)
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

  /**
   * Parse a string expression as a hex-coded unsigned byte.
   * 
   * @return A byte in the range 0 - 255 if sign is ignored.
   */
  public static byte parseUnsignedByte (String byteExpr)
    throws InvalidFormatException
  {
    if (byteExpr.length () > 2)
      throw new InvalidFormatException
        ("Byte value too long: \"" + byteExpr + "\"");
    
    int value = 0;
    
    for (int i = 0; i < byteExpr.length (); i++)
    {
      char c = byteExpr.charAt (i);
      
      int digit;
      
      if (c >= '0' && c <= '9')
        digit = c - '0';
      else if (c >= 'a' && c <= 'f')
        digit = c - 'a' + 10;
      else if (c >= 'A' && c <= 'F')
        digit = c - 'A' + 10;
      else
        throw new InvalidFormatException ("Not a valid hex character: " + c);
      
      value = (value << 4) | digit;
    }
  
    return (byte)value;
  }

  /**
   * Parse a numeric int, long or double value. e.g. 32L, 3.14, 42.
   */
  public static Number parseNumberValue (String valueExpr)
    throws InvalidFormatException
  {
    try
    {
      if (valueExpr.indexOf ('.') != -1)
        return Double.valueOf (valueExpr);
      else if (valueExpr.endsWith ("L") || valueExpr.endsWith ("l"))
        return Long.decode (valueExpr.substring (0, valueExpr.length () - 1));
      else
        return Integer.decode (valueExpr);
    } catch (NumberFormatException ex)
    {
      throw new InvalidFormatException ("Invalid number: " + valueExpr);
    }
  }

  /**
   * Parse a string value in the format "string", allowing escaped "'s
   * inside the string.
   */
  public static String parseStringValue (String valueExpr)
    throws InvalidFormatException
  {
    int last = findFirstNonEscaped (valueExpr, 1, '"');
    
    if (last == -1)
      throw new InvalidFormatException ("Missing terminating \" in string");
    else if (last != valueExpr.length () - 1)
      throw new InvalidFormatException ("Extra characters following string");
    
    return stripBackslashes (valueExpr.substring (1, last));
  }

  /**
   * Parse an opaque value expression e.g. [00 0f 01]. 
   */
  public static byte [] parseOpaqueValue (String valueExpr)
    throws InvalidFormatException
  {
    if (valueExpr.length () < 2)
      throw new InvalidFormatException ("Opaque value too short");
    else if (valueExpr.charAt (0) != '[')
      throw new InvalidFormatException ("Missing '[' at start of opaque");
    
    int closingBrace = valueExpr.indexOf (']');
    
    if (closingBrace == -1)
      throw new InvalidFormatException ("Missing closing \"]\"");
    else if (closingBrace != valueExpr.length () - 1)
      throw new InvalidFormatException ("Junk at end of oqaque value");
  
    String [] byteExprs = valueExpr.substring (1, closingBrace).split (" +");
    byte [] bytes = new byte [byteExprs.length];
    
    try
    {
      for (int i = 0; i < byteExprs.length; i++)
        bytes [i] = parseUnsignedByte (byteExprs [i]);
    } catch (NumberFormatException ex)
    {
      throw new InvalidFormatException ("Invalid byte value: " + ex.getMessage ());
    }
    
    return bytes;
  }
  
  public static int findFirstNonEscaped (String str, int start, char toFind)
  {
    boolean escaped = false;
    
    for (int i = start; i < str.length (); i++)
    {
      char c = str.charAt (i);
      
      if (c == '\\')
      {
        escaped = true;
      } else
      {
        if (!escaped && c == toFind)
          return i;
        
        escaped = false;
      }
    }
    
    return -1;
  }
  
  public static String stripBackslashes (String text)
  {
    if (text.indexOf ('\\') != -1)
    {
      StringBuilder buff = new StringBuilder (text.length ());
      
      for (int i = 0; i < text.length (); i++)
      {
        char c = text.charAt (i);
        
        if (c != '\\')
          buff.append (c);
      }
      
      text = buff.toString ();
    }
    
    return text;
  }
  
  /**
   * String.split ("") returns {""} rather than {} like you might
   * expect: this returns empty array on "".
   */
  public static String [] split (String text, String regex)
  {
    if (text.length () == 0)
      return EMPTY_STRING_ARRAY;
    else
      return text.split (regex);
  }

  /**
   * Generate human friendly string dump of a Map.
   */
  public static String mapToString (Map<?, ?> map)
  {
    StringBuffer str = new StringBuffer ();
    boolean first = true;
    
    for (Map.Entry<?, ?> entry : map.entrySet ())
    {
      if (!first)
        str.append (", ");
      
      first = false;
      
      str.append ('{');
      str.append (entry.getKey ()).append (" = ").append (entry.getValue ());
      str.append ('}');
    }
    
    return str.toString ();
  }
}
