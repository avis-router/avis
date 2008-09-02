package org.avis.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.avis.io.messages.Notify;
import org.avis.util.InvalidFormatException;
import org.avis.util.Text;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

import static org.avis.util.Streams.bufferedReaderFor;
import static org.avis.util.Text.className;
import static org.avis.util.Text.findFirstNonEscaped;
import static org.avis.util.Text.formatNotification;
import static org.avis.util.Text.stringToValue;
import static org.avis.util.Text.stripBackslashes;

/**
 * A notification sent via an Elvin router. A notification is a set of
 * field name/value pairs. Field values may be one of the following
 * types:
 * 
 * <ul>
 * <li><tt>int     => Elvin Int32</tt>
 * <li><tt>long    => Elvin Int64</tt>
 * <li><tt>double  => Elvin Real64</tt>
 * <li><tt>String  => Elvin String</tt>
 * <li><tt>byte [] => Elvin Opaque</tt>
 * </ul>
 * 
 * <h3>Byte Arrays</h3>
 * <p>
 * For efficiency, byte arrays passed in via the set () methods,
 * constuctors, and clone () are not copied before being added to this
 * object, nor are they copied before being returned by the get ()
 * methods. Please note that modifying a byte array that is part of a
 * notification can cause undefined behaviour: treat all values of a
 * notification as immutable.
 * 
 * @author Matthew Phillips
 */
public final class Notification
  implements Cloneable, Iterable<Entry<String, Object>>
{
  protected Map<String, Object> attributes;
  
  /**
   * Create an empty notification.
   */
  public Notification ()
  {
    this.attributes = new HashMap<String, Object> ();
  }

  /**
   * Internal constructor for quickly creating a notification from an Elvin
   * notify message. 
   */
  Notification (Notify message)
  {
    this.attributes = message.attributes;
  }
  
  /**
   * Internal constructor that bypasses key/value checks.
   * 
   * @param attributes attributes to assign in new notification.
   * @param skipChecking Dummy.  
   */
  Notification (Map<String, Object> attributes, boolean skipChecking)
  {
    this.attributes = attributes;
  }
  
  /**
   * Create a notification from an array of name/value pairs.
   * 
   * @throws IllegalArgumentException if attributes do not represent a
   *           valid notification.
   */
  public Notification (Object... attributes)
    throws IllegalArgumentException
  {
    this ();
    
    if (attributes.length % 2 != 0)
      throw new IllegalArgumentException
        ("Attributes must be a list of name/value pairs");
    
    for (int i = 0; i < attributes.length; i += 2)
      set (checkField (attributes [i]), attributes [i + 1]);
  }

  /**
   * Create a notification from the values in a map.
   * 
   * @param map The map to copy.
   * 
   * @throws IllegalArgumentException if one of the map values is not
   *           a valid type.
   */
  public Notification (Map<?, ?> map)
    throws IllegalArgumentException
  {
    this ();
    
    setAll (map);
  }

  /**
   * Create an instance from an string encoded notification.
   * See {@link #parse(Notification, Reader)}.
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
   * Create an instance from an encoded notification read from a stream.
   * See {@link #parse(Notification, Reader)}.
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
   * is compatible with that used by the <code>ec</code> and
   * <code>ep</code> utilities. For example:
   * 
   * <pre>
   *   An-Int32: 42
   *   An-Int64: 24L
   *   A-Real64: 3.14
   *   String:   &quot;String with a \&quot; in it&quot;
   *   Opaque:   [01 02 0f ff]
   *   A field with a \: in it: 1
   * </pre>
   * 
   * The parser ignores lines starting with "$" and stops on end of
   * stream or "---".
   * 
   * @param ntfn The notification to add values to.
   * @param in The source to read the expression from.
   * @throws IOException If reader throws an IO exception.
   * @throws InvalidFormatException If there is an error in the format
   *           of the expression. The notification may contain a
   *           partial set of values already successfully read.
   */
  public static void parse (Notification ntfn, Reader in)
    throws IOException, InvalidFormatException
  {
    BufferedReader reader = bufferedReaderFor (in);
    String line = null;
    
    try
    {
      while ((line = nextLine (reader)) != null)
        parseLine (ntfn, line);
      
    } catch (InvalidFormatException ex)
    {
      throw new InvalidFormatException
        ("Notification line \"" + line + "\": " + ex.getMessage ());
    }
  }

  /**
   * Read the next line of a notification.
   * 
   * @return The next line, or null if at eof or the notification
   *         "---" terminator.
   */
  private static String nextLine (BufferedReader in)
    throws IOException
  {
    String line;
    
    do
    {
      line = in.readLine ();
      
      if (line != null)
        line = line.trim ();
    } while (line != null && (line.startsWith ("$") || line.length () == 0));
    
    if (line != null)
    {
      if (line.startsWith ("---"))
        line = null;
    }
    
    return line;
  }

  private static void parseLine (Notification ntfn, String line)
    throws InvalidFormatException
  {
    int colon = findFirstNonEscaped (line, ':');
    
    if (colon == -1)
      throw new InvalidFormatException ("No \":\" separating name and value");
    else if (colon == line.length () - 1)
      throw new InvalidFormatException ("Missing value");
    
    String name = stripBackslashes (line.substring (0, colon).trim ());
    String valueExpr = line.substring (colon + 1).trim ();
    
    ntfn.attributes.put (name, stringToValue (valueExpr));
  }

  /**
   * Remove all attributes from the notification.
   */
  public void clear ()
  {
    attributes.clear ();
  }

  @Override
  public Notification clone ()
  {
    return new Notification 
      (new HashMap<String, Object> (this.attributes), true);
  }
  
  /**
   * Generate a string value of the notification. The format is
   * compatible with that used by the ec/ep commands. See
   * {@link #parse(Notification, Reader)} for an example.
   * 
   * @see Text#formatNotification(Map)
   */
  @Override
  public String toString ()
  {
    return formatNotification (attributes);
  }

  /**
   * Test if this notification has a field with a given name.
   */
  public boolean hasField (String name)
  {
    return attributes.containsKey (name);
  }

  /**
   * The set of names in the notification. This is a live, unmodifiable set.
   */
  public Set<String> names ()
  {
    return unmodifiableSet (attributes.keySet ());
  }
  
  /**
   * The set of values in the notification. This is a live,
   * unmodifiable collection.
   */
  public Collection<Object> values ()
  {
    return unmodifiableCollection (attributes.values ());
  }
  
  /**
   * A live, unmodifiable Map view of this notification.
   */
  public Map<String, Object> asMap ()
  {
    return unmodifiableMap (attributes);
  }
  
  /**
   * Get the fields/values of this notification as a unmodifiable,
   * live set of java.util.Map.Entry's that can be iterated over.
   * 
   * @see #iterator()
   */
  public Set<Entry<String, Object>> entrySet ()
  {
    return unmodifiableSet (attributes.entrySet ());
  }
  
  /**
   * Create an iterator over this notification.
   */
  public Iterator<Entry<String, Object>> iterator ()
  {
    return entrySet ().iterator ();
  }

  @Override
  public boolean equals (Object o)
  {
    if (o instanceof Notification)
      return equals ((Notification)o);
    else
      return false;
  }

  /**
   * Compare two notifications.
   */
  public boolean equals (Notification ntfn)
  {
    /*
     * NB: Cannot use HashMap.equals () as it does not compare byte
     * arrays by value.
     */
    
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

  @Override
  public int hashCode ()
  {
    // can't use HashMap.hashCode () for same reason as can't use equals ().
    return attributes.keySet ().hashCode ();
  }

  /**
   * True if size () == 0.
   */
  public boolean isEmpty ()
  {
    return attributes.isEmpty ();
  }

  /**
   * The number of name/value pairs in the notification.
   */
  public int size ()
  {
    return attributes.size ();
  }
  
  /**
   * Copy all values in a map into this notification.
   * 
   * @param map The map to copy.
   * 
   * @throws IllegalArgumentException if a key in the map is not a
   *                 string, or a value is not a string, integer,
   *                 long, double or byte array. Some values may
   *                 already have been added to the notification.
   */
  public void setAll (Map<?, ?> map)
  {
    for (Map.Entry<?, ?> entry : map.entrySet ())
      set (checkField (entry.getKey ()), entry.getValue ());
  }

  /**
   * Set a field value.
   * 
   * @param name The field name.
   * @param value The value, or null to clear field.
   * 
   * @throws IllegalArgumentException if value is not a string,
   *           integer, long, double or byte array.
   */
  public void set (String name, Object value)
    throws IllegalArgumentException
  {
    if (value == null)
      attributes.remove (name);
    else
      attributes.put (name, checkValue (value));
  }

  /**
   * Set an integer value.
   * 
   * @param name The field name.
   * @param value The value.
   * 
   * @see #set(String, Object)
   */
  public void set (String name, int value)
  {
    attributes.put (name, value);
  }
  
  /**
   * Set a long value.
   * 
   * @param name The field name.
   * @param value The value.
   * 
   * @see #set(String, Object)
   */
  public void set (String name, long value)
  {
    attributes.put (name, value);
  }
  
  /**
   * Set a double value.
   * 
   * @param name The field name.
   * @param value The value.
   * 
   * @see #set(String, Object)
   */
  public void set (String name, double value)
  {
    attributes.put (name, value);
  }
  
  /**
   * Set a string value.
   * 
   * @param name The field name.
   * @param value The value.
   * 
   * @see #set(String, Object)
   */
  public void set (String name, String value)
  {
    attributes.put (name, value);
  }
  
  /**
   * Set an opaque byte array value.
   * 
   * @param name The field name.
   * @param value The value.
   * 
   * @see #set(String, Object)
   */
  public void set (String name, byte [] value)
  {
    attributes.put (name, value);
  }

  /**
   * Remove (unset) a value.
   * 
   * @param name the field name.
   */
  public void remove (String name)
  {
    attributes.remove (name);
  }

  /**
   * Get a field value.
   * 
   * @param name The field name.
   * 
   * @return The value, or null if no value.
   */
  public Object get (String name)
  {
    return attributes.get (name);
  }

  /**
   * Get a string value.
   * 
   * @param name The field name.
   * 
   * @return The value, or null if no value.
   * 
   * @throws IllegalArgumentException if value is not a string.
   */
  public String getString (String name)
    throws IllegalArgumentException
  {
    return get (name, String.class);
  }
  
  /**
   * Get an integer value.
   * 
   * @param name The field name.
   * 
   * @return The value.
   * 
   * @throws IllegalArgumentException if value is not an integer or is null.
   */
  public int getInt (String name)
    throws IllegalArgumentException
  {
    return getNonNull (name, Integer.class);
  }

  /**
   * Get a long value.
   * 
   * @param name The field name.
   * 
   * @return The value.
   * 
   * @throws IllegalArgumentException if value is not a long or is null.
   */
  public long getLong (String name)
    throws IllegalArgumentException
  {
    return getNonNull (name, Long.class);
  }
  
  /**
   * Get a double value.
   * 
   * @param name The field name.
   * 
   * @return The value.
   * 
   * @throws IllegalArgumentException if value is not a double or is null.
   */
  public double getDouble (String name)
    throws IllegalArgumentException
  {
    return getNonNull (name, Double.class);
  }
  
  /**
   * Get an opaque byte array value.
   * 
   * @param name The field name.
   * 
   * @return The value, or null for no value.
   * 
   * @throws IllegalArgumentException if value is not a byte array.
   */
  public byte [] getOpaque (String name)
    throws IllegalArgumentException
  {
    return get (name, byte [].class);
  }
  
  /**
   * Shortcut to get a non-null value of specified type, or generate
   * an exception.
   * 
   * @param name The field name.
   * @param type The required type.
   * @return The value.
   * 
   * @throws IllegalArgumentException if there is no value for the
   *           field or the value is not the required type.
   */
  public <E> E require (String name, Class<E> type)
    throws IllegalArgumentException
  {
    return getNonNull (name, type);
  }
  
  private <T> T getNonNull (String name, Class<T> type)
  {
    T value = get (name, type);
    
    if (value == null)
      throw new IllegalArgumentException ("No value for \"" + name + "\"");
    else
      return value;
  }
  
  @SuppressWarnings ("unchecked")
  private <T> T get (String name, Class<T> type)
  {
    Object value = attributes.get (name);
    
    if (value == null || type.isAssignableFrom (value.getClass ()))
    {
      return (T)value;
    } else
    {
      throw new IllegalArgumentException
        ("\"" + name + "\" does not contain a " + typeName (type) + " value");
    }
  }

  private static String typeName (Class<?> type)
  {
    if (type == byte [].class)
      return "byte array";
    else
      return className (type).toLowerCase ();
  }
  
  private static Object checkValue (Object value)
    throws IllegalArgumentException
  {
    if ((value instanceof String ||
         value instanceof Integer ||
         value instanceof Long ||
         value instanceof Double ||
         value instanceof byte []))
    {
      return value;
    } else
    {
      throw new IllegalArgumentException
        ("Value must be a string, integer, long, double or byte array");
    }
  }
  
  private static String checkField (Object field)
  {
    if (field instanceof String)
    {
      return (String)field;
    } else
    {
      throw new IllegalArgumentException 
        ("Name must be a string: \"" + field + "\"");
    }
  }
}
