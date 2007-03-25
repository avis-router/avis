package org.avis.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.avis.util.InvalidFormatException;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

import static org.avis.util.Format.appendBytes;
import static org.avis.util.Format.appendEscaped;
import static org.avis.util.Format.findFirstNonEscaped;
import static org.avis.util.Format.parseNumberValue;
import static org.avis.util.Format.parseOpaqueValue;
import static org.avis.util.Format.parseStringValue;
import static org.avis.util.Format.stripBackslashes;

public class Notification implements Map<String, Object>, Cloneable
{
  private Map<String, Object> attributes;
  
  public Notification ()
  {
    this.attributes = new HashMap<String, Object> ();
  }

  public Notification (Map<String, Object> attributes)
  {
    // todo check attribute values
    this.attributes = attributes;
  }
  
  /**
   * Create a notification from an input expression.
   * See {{@link #parse(Notification, Reader)}.
   */
  public Notification (String ntfnExpr)
    throws InvalidFormatException
  {
    this ();
    
    try
    {
      parse (this, new StringReader (ntfnExpr));
    } catch (IOException ex)
    {
      // a StringReader should never throw this
      throw new RuntimeException (ex);
    }
  }
  
  /**
   * Create a notification from an input expression.
   * See {{@link #parse(Notification, Reader)}.
   */
  public Notification (Reader in)
    throws IOException, InvalidFormatException
  {
    this ();
    
    parse (this, in);
  }
  
  /**
   * Parse an expression representing a notification and populate the
   * given notification with the values. The format of this expression
   * is compatible with that used by the ec/ep utilities. For example:
   * 
   * <pre>
   *   An int32: 42
   *   An int64: 24L
   *   Real64: 3.14
   *   String: &quot;String with a \&quot; in it&quot;
   *   Opaque: [01 02 0f ff]
   *   A field with a \: in it: 1
   * </pre>
   * 
   * @param ntfn The notification to add values to.
   * @param reader The source to read the expression from.
   * @throws IOException If reader throws an IO exception.
   * @throws InvalidFormatException If there is an error in the format
   *           of the expression. The notification may contain a
   *           partial set of values already successfully read.
   */
  public static void parse (Notification ntfn, Reader reader)
    throws IOException, InvalidFormatException
  {
    BufferedReader in = new BufferedReader (reader);
    
    String line = null;
    
    try
    {
      while ((line = in.readLine ()) != null)
        parseLine (ntfn, line.trim ());
    } catch (InvalidFormatException ex)
    {
      throw new InvalidFormatException
        ("Notification line \"" + line + "\": " + ex.getMessage ());
    }
  }

  private static void parseLine (Notification ntfn, String line)
    throws InvalidFormatException
  {
    int colon = findFirstNonEscaped (line, 0, ':');
    
    if (colon == -1)
      throw new InvalidFormatException ("No \":\" separating name and value");
    else if (colon == line.length () - 1)
      throw new InvalidFormatException ("Missing value");
    
    String name = stripBackslashes (line.substring (0, colon).trim ());
    String valueExpr = line.substring (colon + 1).trim ();
    Object value;
    
    char firstChar = valueExpr.charAt (0);
    
    if (firstChar == '"')
      value = parseStringValue (valueExpr);
    else if (firstChar >= '0' && firstChar <= '9')
      value = parseNumberValue (valueExpr);
    else if (firstChar == '[')
      value = parseOpaqueValue (valueExpr);
    else
      throw new InvalidFormatException
        ("Unrecognised value expression: \"" + valueExpr + "\"");
    
    ntfn.put (name, value);
  }

  public void clear ()
  {
    attributes.clear ();
  }

  @Override
  public Object clone ()
    throws CloneNotSupportedException
  {
    Notification copy = (Notification)super.clone ();
    
    copy.attributes = new HashMap<String, Object> (attributes);
    
    return copy;
  }
  
  /**
   * Generate a string value of the notification. The format is
   * compatible with that used by the ec/ep commands. See
   * {@link #parse(Notification, Reader)} for an example.
   */
  @Override
  public String toString ()
  {
    StringBuilder str = new StringBuilder ();
    boolean first = true;
    
    Set<String> names = new TreeSet<String> (CASE_INSENSITIVE_ORDER);
    names.addAll (attributes.keySet ());
    
    for (String name : names)
    {
      if (!first)
        str.append ('\n');
      
      first = false;
      
      appendEscaped (str, name, " :");
      
      str.append (": ");
      
      formatValue (str, attributes.get (name));
    }
    
    return str.toString ();
  }
  
  private static void formatValue (StringBuilder str, Object value)
  {
    if (value instanceof String)
    {
      str.append ('"');
      appendEscaped (str, value.toString (), '"');
      str.append ('"');
    } else if (value instanceof Number)
    {
      str.append (value);
      
      if (value instanceof Long)
        str.append ('L');   
    } else
    {
      str.append ('[');
      appendBytes (str, (byte [])value);
      str.append (']');
    }
  }

  public boolean containsKey (Object key)
  {
    return attributes.containsKey (key);
  }

  public boolean containsValue (Object value)
  {
    return attributes.containsValue (value);
  }

  public Set<Entry<String, Object>> entrySet ()
  {
    return attributes.entrySet ();
  }

  public boolean equals (Object o)
  {
    if (o instanceof Notification)
      return equals ((Notification)o);
    else
      return false;
  }

  /**
   * Compare two notifications. Cannot use HashMap.equals () as it
   * does not compare byte arrays by value.
   */
  public boolean equals (Notification ntfn)
  {
    if (this == ntfn)
    {
      return true;
    } else if (attributes.size () != ntfn.attributes.size ())
    {
      return false;
    } else
    {
      for (Entry<String, Object> entry : attributes.entrySet ())
      {
        if (!valuesEqual (entry.getValue (),
                          ntfn.attributes.get (entry.getKey ())))
          return false;
      }
    }
    
    return true;
  }
  
  private static boolean valuesEqual (Object value1, Object value2)
  {
    if (value1 == value2)
      return true;
    else if (value1 == null || value2 == null)
      return false;
    else if (value1.getClass () != value2.getClass ())
      return false;
    else if (value1 instanceof byte [])
      return Arrays.equals ((byte [])value1, (byte [])value2);
    else
      return value1.equals (value2);
  }

  public int hashCode ()
  {
    // todo opt: get a better hashcode?
    // can't use HashMap.hashCode () for same reason as can't use equals ().
    return attributes.size ();
  }

  public Object get (Object key)
  {
    return attributes.get (key);
  }

  public boolean isEmpty ()
  {
    return attributes.isEmpty ();
  }

  public Set<String> keySet ()
  {
    return attributes.keySet ();
  }

  public Object put (String key, Object value)
  {
    return attributes.put (key, value);
  }

  public void putAll (Map<? extends String, ? extends Object> m)
  {
    attributes.putAll (m);
  }

  public Object remove (Object key)
  {
    return attributes.remove (key);
  }

  public int size ()
  {
    return attributes.size ();
  }

  public Collection<Object> values ()
  {
    return attributes.values ();
  }

  public byte [] getOpaque (String field)
  {
    Object value = get (field);
    
    if (value == null || value instanceof byte [])
      return (byte [])value;
    else
      throw new IllegalArgumentException
        ("\"" + field + "\" does not contain an opaque value");
  }
}
