package org.avis.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import static org.avis.io.LegacyConnectionOptions.legacyToNew;
import static org.avis.io.LegacyConnectionOptions.newToLegacy;
import static org.avis.util.Util.valuesEqual;

/**
 * Connection options sent by the client to the server.
 * <p>
 * 
 * In addition to the options sent by the client to the router
 * detailed below, the router may also add a "Vendor-Identification"
 * option to its reply with a string identifying the router
 * implementation, e.g. "Avis 1.2" or "elvind 4.4.0".
 * 
 * <h2>Standard Elvin Connection Options</h2>
 * 
 * <p>
 * See http://elvin.org/specs for details on client connection
 * options.
 * </p>
 * 
 * <dl>
 * <dt>Packet.Max-Length</dt>
 * <dd>Max packet length acceptable from a client.</dd>
 * 
 * <dt>Subscription.Max-Count</dt>
 * <dd>Maximum number of subscriptions allowed by a single client.</dd>
 * 
 * <dt>Subscription.Max-Length</dt>
 * <dd>Maximum length, in bytes, of any subscription expression.</dd>
 * 
 * <dt>Receive-Queue.Max-Length</dt>
 * <dd>The maximum size of the router's per-client incoming packet
 * queue, in bytes. If the queue exceeds this size, the router will
 * throttle the data stream from the client until the queue drops
 * below this value.</dd>
 * 
 * <dt>TCP.Send-Immediately</dt>
 * <dd>Set whether the TCP NO_DELAY flag is enabled for sockets on
 * the server side. 1 = send immediately (TCP NO_DELAY = true), 0 = do
 * not necessarily send immediately, buffer data for optimal
 * throughput (TCP NO_DELAY = false). Set this to 1 if you experience
 * lag with "real time" applications that require minimal delivery
 * latency, but note that this may result in an overall reduction in
 * throughput.</dd>
 * 
 * <dt>Attribute.Name.Max-Length</dt>
 * <dd>The maximum length, in bytes, of an attribute name.</dd>
 * 
 * <dt>Attribute.Max-Count</dt>
 * <dd>The maximum number of attributes on a notification.</dd>
 * 
 * <dt>Attribute.Opaque.Max-Length</dt>
 * <dd>Maximum length, in bytes, for opaque values.</dd>
 * 
 * <dt>Attribute.String.Max-Length</dt>
 * <dd>Maximum length, in bytes, for opaque values. Note that this
 * value is not the number of characters: some characters may take up
 * to 5 bytes to represent using the required UTF-8 encoding.</dd>
 * 
 * <dt>Receive-Queue.Drop-Policy</dt>
 * <dd>This property describes the desired behaviour of the router's
 * packet receive queue if it exceeds the negotitated maximum size.
 * Values: "oldest", "newest", "largest", "fail"</dd>
 * 
 * <dt>Send-Queue.Drop-Policy</dt>
 * <dd>This property describes the desired behaviour of the router's
 * packet send queue if it exceeds the negotitated maximum size.
 * Values: "oldest", "newest", "largest", "fail"</dd>
 * 
 * <dt>Send-Queue.Max-Length</dt>
 * <dd>The maximum length (in bytes) of the routers send queue.</dd>
 * </dl>
 * 
 * @author Matthew Phillips
 */
public final class ConnectionOptions
{
  private static final Map<String, Object> EMPTY_MAP = emptyMap ();

  /**
   * An immutable set of empty options.
   */
  public static final ConnectionOptions EMPTY_OPTIONS =
    new ConnectionOptions (EMPTY_MAP);

  private Map<String, Object> values;
  private boolean includeLegacy;
  
  /**
   * Create an empty instance.
   */
  public ConnectionOptions ()
  {
    this (new HashMap<String, Object> ());
  }
  
  protected ConnectionOptions (Map<String, Object> values)
  {
    this.values = values;
    this.includeLegacy = true;
  }
  
  /**
   * Enable legacy connection option compatibility (default is
   * enabled). This enables compatibility with older Mantara Elvin
   * routers, and should be left active unless you are sure the target
   * router supports new-style options. Enabling this adds a small
   * amount of size to the initial connection request, but no further
   * overhead.
   * 
   * @see #asMapWithLegacy()
   */
  public void includeLegacy (boolean newValue)
  {
    this.includeLegacy = newValue;
  }

  /**
   * Generate a map view of this connection option set, automatically
   * adding legacy connection options as required.
   * 
   * @see #convertLegacyToNew(Map)
   */
  protected Map<String, Object> asMapWithLegacy ()
  {
    if (values.isEmpty ())
      return emptyMap ();
    
    HashMap<String, Object> options = new HashMap<String, Object> ();
    
    for (Map.Entry<String, Object> entry : values.entrySet ())
    {
      if (includeLegacy)
      {
        Object value = entry.getValue ();
        
        /*
         * TCP.Send-Immediately maps to router.coalesce-delay which
         * has opposite meaning.
         */
        if (entry.getKey ().equals ("TCP.Send-Immediately"))
          value = value.equals (0) ? 1 : 0;
        
        options.put (newToLegacy (entry.getKey ()), value);
      }
      
      options.put (entry.getKey (), entry.getValue ());
    }
    
    return options;
  }
  
  /**
   * Convert options that may contain legacy settings to new-style
   * ones as required.
   * 
   * @param legacyOptions input options.
   * @return A set of options with any legacy options mapped to the
   *         new style ones.
   */
  protected static Map<String, Object> convertLegacyToNew 
    (Map<String, Object> legacyOptions)
  {
    if (legacyOptions.isEmpty ())
      return emptyMap ();
    
    HashMap<String, Object> options = new HashMap<String, Object> ();
    
    for (Map.Entry<String, Object> entry : legacyOptions.entrySet ())
    {
      Object value = entry.getValue ();
        
      /*
       * router.coalesce-delay maps to TCP.Send-Immediately which has
       * opposite meaning.
       */
      if (entry.getKey ().equals ("router.coalesce-delay"))
        value = value.equals (0) ? 1 : 0;
      
      options.put (legacyToNew (entry.getKey ()), value);
    }
    
    return options;
  }
  
  /**
   * Generate an immutable, live map of the current options.
   * 
   * @see #asMapWithLegacy()
   */
  public Map<String, Object> asMap ()
  {
    return unmodifiableMap (values);
  }
  
  /**
   * Generate the difference between this option set and an actual set
   * returned by the server.
   */
  protected Map<String, Object> differenceFrom (Map<String, Object> options)
  {
    HashMap<String, Object> diff = new HashMap<String, Object> ();
    
    for (Entry<String, Object> entry : values.entrySet ())
    {
      Object actualValue = options.get (entry.getKey ());
      
      if (!valuesEqual (entry.getValue (), actualValue))
        diff.put (entry.getKey (), actualValue);
    }
    
    return diff;
  }
  
  /**
   * Set a connection option.  
   * 
   * @param name The option name.
   * @param value The value. Must be a string or a number. Use null to clear.
   * 
   * @throws IllegalArgumentException if value is not a string or an integer.
   */
  public void set (String name, Object value)
    throws IllegalArgumentException
  {
    if (value == null)
    {
      values.remove (name);
    } else if (value instanceof String || value instanceof Integer)
    {
      values.put (name, value);
    } else
    {
      throw new IllegalArgumentException
        ("Value must be a string or integer: " + value);
    }
  }
  
  /**
   * Set an integer value.
   */
  public void set (String name, int value)
  {
    values.put (name, value);
  }
  
  /**
   * Set a boolean value. Elvin connection options are actually either
   * strings or integers: this is a shortcut for setting an int value
   * to 0 or 1.
   * 
   * @see #getBoolean(String)
   */
  public void set (String name, boolean value)
  {
    values.put (name, value ? 1 : 0);
  }

  /**
   * Set a string value.
   */
  public void set (String name, String value)
  {
    if (value == null)
      values.remove (name);
    else
      values.put (name, value);
  }
  
  /**
   * Set a number of options at once.
   */
  public void setAll (Map<String, Object> options)
  {
    for (Map.Entry<String, Object> entry : options.entrySet ())
      set (entry.getKey (), entry.getValue ());
  }

  /**
   * Get the value for a connection option, or null if not defined.
   */
  public Object get (String name)
  {
    return values.get (name);
  }
  
  /**
   * Get an integer value.
   * 
   * @param name The option name.
   * 
   * @return The option value.
   * 
   * @throws IllegalArgumentException if the option has no value or is
   *                 not an int.
   */
  public int getInt (String name)
    throws IllegalArgumentException
  {
    return asInt (name, values.get (name));
  }
  
  /**
   * Get an integer value.
   * 
   * @param name The option name.
   * @param defaultValue The default value. This is returned if there
   *                is no value for the option.
   * 
   * @return The option value.
   * 
   * @throws IllegalArgumentException if the option is not an int.
   */
  public int getInt (String name, int defaultValue)
    throws IllegalArgumentException
  {
    Object value = values.get (name);
    
    if (value == null)
      return defaultValue;
    else
      return asInt (name, value);
  }

  /**
   * Get a string value.
   * 
   * @param name The option name.
   * 
   * @return The option value.
   * 
   * @throws IllegalArgumentException if the option has no value or is
   *                 not a string.
   */
  public String getString (String name)
    throws IllegalArgumentException
  {
    return asString (name, values.get (name));
  }
  
  /**
   * Get a string value.
   * 
   * @param name The option name.
   * @param defaultValue The default value. This is returned if there
   *                is no value for the option.
   * 
   * @return The option value.
   * 
   * @throws IllegalArgumentException if the option is not a string.
   */
  public String getString (String name, String defaultValue)
    throws IllegalArgumentException
  {
    Object value = values.get (name);
    
    if (value == null)
      return defaultValue;
    else
      return asString (name, value);
  }
  
  /**
   * Get a boolean value. NB: Elvin connection options are either
   * strings or integers: this is actually a shortcut for getting an
   * int value in the range 0 to 1.
   * 
   * @param name The option name.
   * @return The boolean value.
   * 
   * @throws IllegalArgumentException if the option is not an int in
   *                 the range 0-1.
   */
  public boolean getBoolean (String name)
    throws IllegalArgumentException
  {
    return asBoolean (name, values.get (name));
  }
  
  /**
   * Get a boolean value. Elvin connection options are actually either
   * strings or integers: this is a shortcut for getting an int value
   * in the range 0 or 1.
   * 
   * @param name The option name.
   * @param defaultValue The value to return if there is no value set.
   * @return The boolean value.
   * 
   * @throws IllegalArgumentException if the option is not an int in
   *                 the range 0-1.
   */
  public boolean getBoolean (String name, boolean defaultValue)
    throws IllegalArgumentException
  {
    Object value = values.get (name);
    
    if (value == null)
      return defaultValue;
    else
      return asBoolean (name, value);
  }
  
  private static int asInt (String name, Object value)
  {
    if (value instanceof Integer)
      return (Integer)value;
    else
      throw new IllegalArgumentException 
        ("\"" + name +"\" is not an integer: " + value);
  }
  
  private static String asString (String name, Object value)
  {
    if (value instanceof String)
      return (String)value;
    else
      throw new IllegalArgumentException 
        ("\"" + name +"\" is not a string: " + value);
  }
  
  private static boolean asBoolean (String name, Object value)
  {
    int intValue = asInt (name, value);
    
    if (intValue < 0 || intValue > 1)
      throw new IllegalArgumentException 
        ("\"" + name +"\" is not a boolean-valued integer: " + value);
    
    return intValue == 1;
  }
}
