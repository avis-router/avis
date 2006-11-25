package org.avis.net;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import static org.avis.Common.K;
import static org.avis.Common.MAX;
import static org.avis.Common.MB;

/**
 * Handles Avis connection options: definition, validation and legacy
 * compatibility.
 * <p>
 * 
 * From Sec 7.5:
 * 
 * <pre>
 *      Name                        |  Type    |  Min   Default      Max
 *      ----------------------------+----------+-------------------------
 *      Attribute.Max-Count         |  int32   |    64     256     2**31
 *      Attribute.Name.Max-Length   |  int32   |    64    2048     2**31
 *      Attribute.Opaque.Max-Length |  int32   |    1K      1M     2**31
 *      Attribute.String.Max-Length |  int32   |    1K      1M     2**31
 *      Packet.Max-Length           |  int32   |
 *      Receive-Queue.Drop-Policy   |  string  |
 *      Receive-Queue.High-Water    |  int32   |
 *      Receive-Queue.Low-Water     |  int32   |
 *      Receive-Queue.Max-Length    |  int32   |
 *      Send-Queue.Drop-Policy      |  string  |
 *      Send-Queue.High-Water       |  int32   |
 *      Send-Queue.Low-Water        |  int32   |
 *      Send-Queue.Max-Length       |  int32   |
 *      Subscription.Max-Count      |  int32   |
 *      Subscription.Max-Length     |  int32   |
 *      Supported-Key-Schemes       |  string  |
 *      ----------------------------+----------+-------------------------
 * </pre>
 * 
 * Compatibility:
 * 
 * <pre>
 *      Standard Name               | Compatibility Name
 *      ----------------------------+------------------------------------
 *      Attribute.Max-Count         | router.attribute.max-count
 *      Attribute.Name.Max-Length   | router.attribute.name.max-length
 *      Attribute.Opaque.Max-Length | router.attribute.opaque.max-length
 *      Attribute.String.Max-Length | router.attribute.string.max-length
 *      Packet.Max-Length           | router.packet.max-length
 *      Receive-Queue.Drop-Policy   | router.recv-queue.drop-policy
 *      Receive-Queue.High-Water    | router.recv-queue.high-water
 *      Receive-Queue.Low-Water     | router.recv-queue.low-water
 *      Receive-Queue.Max-Length    | router.recv-queue.max-length
 *      Send-Queue.Drop-Policy      | router.send-queue.drop-policy
 *      Send-Queue.High-Water       | router.send-queue.high-water
 *      Send-Queue.Low-Water        | router.send-queue.low-water
 *      Send-Queue.Max-Length       | router.send-queue.max-length
 *      Subscription.Max-Count      | router.subscription.max-count
 *      Subscription.Max-Length     | router.subscription.max-length
 *      Supported-Key-Schemes       | router.supported-keyschemes
 *      Vendor-Identification       | router.vendor-identification
 *      ----------------------------+------------------------------------
 * </pre>
 * 
 * @author Matthew Phillips
 */
public class ConnectionOptions
{
  private static final Map<String, Object> EMPTY_OPTIONS = Collections.emptyMap ();
  
  private static final Map<String, Object> DEFAULT_VALUES;
  private static final Map<String, Object> VALIDATION;
  
  // map compatibility names <-> new ones
  private static final Map<String, String> COMPAT_TO_NEW;
  private static final Map<String, String> NEW_TO_COMPAT;
  
  static
  {
    DEFAULT_VALUES = new HashMap<String, Object> ();
    VALIDATION = new HashMap<String, Object> ();
    COMPAT_TO_NEW = new HashMap<String, String> ();
    NEW_TO_COMPAT = new HashMap<String, String> ();
    
    // required for all implementations
    defineOption ("Packet.Max-Length", 1*K, 1*MB, MAX);
    
    /*
     * todo: we only enforce max packet length, which by implication
     * limits the values below. The correct min, default, max values
     * are currently commented out and replaced with MAX to avoid
     * lying to clients that actually care about these.
     */
    // defineOption ("Attribute.Max-Count", 64, 256, MAX);    
    // defineOption ("Attribute.Name.Max-Length", 64, 2*K, MAX);
    // defineOption ("Attribute.Opaque.Max-Length", 1*K, 1*MB, MAX);
    // defineOption ("Attribute.String.Max-Length", 1*K, 1*MB, MAX);
    // defineOption ("Subscription.Max-Count", 1*K, 2*K, MAX);
    // defineOption ("Subscription.Max-Length", 1*K, 2*K, MAX);
    defineOption ("Attribute.Max-Count", MAX, MAX, MAX);    
    defineOption ("Attribute.Name.Max-Length", MAX, MAX, MAX);
    defineOption ("Attribute.Opaque.Max-Length", MAX, MAX, MAX);
    defineOption ("Attribute.String.Max-Length", MAX, MAX, MAX);
    defineOption ("Subscription.Max-Length", MAX, MAX, MAX);
    
    // todo: enforce Subscription.Max-Count
    defineOption ("Subscription.Max-Count", 1*K, 2*K, MAX);

    defineOption ("Receive-Queue.Max-Length", 1*K, 1*MB, 1*MB);

    // todo: enforce following queue-related options
    defineOption ("Receive-Queue.Drop-Policy",
                  "oldest", "newest", "largest", "fail");
    defineOption ("Receive-Queue.High-Water", MAX, MAX, MAX);
    defineOption ("Receive-Queue.Low-Water", MAX, MAX, MAX);
    
    defineOption ("Send-Queue.Drop-Policy",
                  "oldest", "newest", "largest", "fail");
    defineOption ("Send-Queue.High-Water", MAX, MAX, MAX);
    defineOption ("Send-Queue.Low-Water", MAX, MAX, MAX);
    defineOption ("Send-Queue.Max-Length", MAX, MAX, MAX);

    defineOption ("Supported-Key-Schemes", "SHA-1");
    
    // optional
    // todo: need to decide on new name for Network.Coalesce-Delay
    defineOption ("Network.Coalesce-Delay", 0, 1, 1);
    
    // compatibility mappings
    defineCompatOption ("router.attribute.max-count",
                        "Attribute.Max-Count");
    defineCompatOption ("router.attribute.name.max-length",
                        "Attribute.Name.Max-Length");
    defineCompatOption ("router.attribute.opaque.max-length",
                        "Attribute.Opaque.Max-Length");
    defineCompatOption ("router.attribute.string.max-length",
                        "Attribute.String.Max-Length");
    defineCompatOption ("router.packet.max-length",
                        "Packet.Max-Length");
    defineCompatOption ("router.recv-queue.drop-policy",
                        "Receive-Queue.Drop-Policy");
    defineCompatOption ("router.recv-queue.high-water",
                        "Receive-Queue.High-Water");
    defineCompatOption ("router.recv-queue.low-water",
                        "Receive-Queue.Low-Water");
    defineCompatOption ("router.recv-queue.max-length",
                        "Receive-Queue.Max-Length");
    defineCompatOption ("router.send-queue.drop-policy",
                        "Send-Queue.Drop-Policy");
    defineCompatOption ("router.send-queue.high-water",
                        "Send-Queue.High-Water");
    defineCompatOption ("router.send-queue.low-water",
                        "Send-Queue.Low-Water");
    defineCompatOption ("router.send-queue.max-length",
                        "Send-Queue.Max-Length");
    defineCompatOption ("router.subscription.max-count",
                        "Subscription.Max-Count");
    defineCompatOption ("router.subscription.max-length",
                        "Subscription.Max-Length");
    defineCompatOption ("router.supported-keyschemes",
                        "Supported-Key-Schemes");
    defineCompatOption ("router.vendor-identification",
                        "Vendor-Identification");
        
    defineCompatOption ("router.coalesce-delay",
                        "Network.Coalesce-Delay");
  }
  
  private Map<String, Object> requestedOptions;
  private Map<String, Object> options;
  
  public ConnectionOptions ()
  {
    this (EMPTY_OPTIONS);
  }
  
  public ConnectionOptions (Map<String, Object> requestedOptions)
  {
    this.requestedOptions = requestedOptions;
    this.options = validate (requestedOptions);
  }
  
  /**
   * Put an option value.
   * 
   * @param name The option name.
   * @param value The new value.
   * 
   * @see #putWithCompat(String, Object)
   */
  public void put (String name, Object value)
  {
    if (!(value instanceof Integer || value instanceof String))
      throw new IllegalArgumentException
        ("Value must be an integer or a string: " + value);
    
    options.put (name, value);
  }
  
  /**
   * Put a value both under the standard option name and its
   * older compatible name (if any).
   * 
   * @param name The option name.
   * @param value The new value.
   * 
   * @see #put(String, Object)
   */
  public void putWithCompat (String name, Object value)
  {
    put (name, value);
    
    String oldName = NEW_TO_COMPAT.get (name);
    
    if (oldName != null)
      put (oldName, value);
  }
  
  /**
   * Get the defined or default value for an option name.
   */
  public Object get (String name)
  {
    Object value = options.get (name);
    
    if (value == null)
      value = DEFAULT_VALUES.get (name);
    
    return value;
  }

  /** 
   * Get the default value for an option.
   * 
   * @param name The option name.
   */
  public static Object getDefault (String name)
  {
    return DEFAULT_VALUES.get (compatToNew (name));
  }
  
  public static int getDefaultInt (String name)
  {
    return intValue (name, getDefault (name));
  }

  public int getInt (String name)
  {
    return intValue (name, get (name));
  }
  
  public void remove (String name)
  {
    options.remove (name);
  }
  
  private static int intValue (String name, Object value)
  {
    if (value instanceof Integer)
      return (Integer)value;
    else
      throw new IllegalArgumentException
        (name + " does not refer to an integer value: " + value);
  }

  /**
   * Generate an accepted list of options based on the client's
   * requested set. Handles mapping of option names for backward
   * compatibility.
   */
  public Map<String, Object> accepted ()
  {
    HashMap<String, Object> accepted = new HashMap<String, Object> ();
    
    for (Entry<String, Object> entry : options.entrySet ())
      accepted.put (clientCompatOption (entry.getKey ()), entry.getValue ());
    
    return accepted;
  }

  /**
   * Define a int-valued option.
   * 
   * @param option The option name.
   * @param min min value
   * @param defaultValue The default value
   * @param max max value
   */
  private static void defineOption (String option,
                                    int min, int defaultValue, int max)
  {
    DEFAULT_VALUES.put (option, defaultValue);
    VALIDATION.put (option, new int [] {min, max});
  }
  
  /**
   * Define a string-valued option.
   * 
   * @param option The option name.
   * @param defaultValue The default value.
   * @param values Valid values (other than default).
   */
  private static void defineOption (String option, String defaultValue,
                                    String... values)
  {
    DEFAULT_VALUES.put (option, defaultValue);
    
    HashSet<String> valueSet = new HashSet<String> (Arrays.asList (values));
    valueSet.add (defaultValue);

    VALIDATION.put (option, valueSet);
  }
  
  /**
   * Define a compatibility option for older versions of the
   * router/client.
   * 
   * @param oldOption The old option name.
   * @param newOption The new option name.
   */
  private static void defineCompatOption (String oldOption, String newOption)
  {
    COMPAT_TO_NEW.put (oldOption, newOption);
    NEW_TO_COMPAT.put (newOption, oldOption);
  }

  /**
   * Process connection options and return the valid set supported by
   * the router.
   */
  private static Map<String, Object> validate (Map<String, Object> options)
  {
    HashMap<String, Object> validOptions = new HashMap<String, Object> ();
    
    for (Entry<String, Object> option : options.entrySet ())
      validateOption (validOptions,
                      compatToNew (option.getKey ()), option.getValue ());
    
    return validOptions;
  }
  
  private static void validateOption (Map<String, Object> options,
                                      String name, Object value)
  {
    if (DEFAULT_VALUES.containsKey (name))
    {
      boolean valid;
      
      if (DEFAULT_VALUES.get (name).getClass () != value.getClass ())
      {
        valid = false;
      } else if (value instanceof Integer)
      {
        int intValue = (Integer)value;
        int [] minMax = (int [])VALIDATION.get (name);
        
        valid = intValue >= minMax [0] && intValue <= minMax [1];
      } else if (value instanceof String)
      {
        valid = ((Set)VALIDATION.get (name)).contains (value);
      } else
      {
        // should not be able to get here if options defined correctly
        throw new Error ();
      }
      
      options.put (name, valid ? value : DEFAULT_VALUES.get (name));
    }    
  }
  
  /**
   * Map a backwards-compatible optiont to its new name.
   */
  private static String compatToNew (String option)
  {
    if (COMPAT_TO_NEW.containsKey (option))
      return COMPAT_TO_NEW.get (option);
    else
      return option;
  }
  
  /**
   * Convert a new-style option to the old style if that's what client
   * originally asked for.
   */
  private String clientCompatOption (String name)
  {
    if (!requestedOptions.containsKey (name) &&
        NEW_TO_COMPAT.containsKey (name))
    {
      return NEW_TO_COMPAT.get (name);
    } else
    {
      return name;
    }
  }
}
