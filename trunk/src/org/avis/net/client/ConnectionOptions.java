package org.avis.net.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.Collections.unmodifiableMap;

import static org.avis.util.Util.valuesEqual;

/**
 * Connection options sent by the client to the server.
 * 
 * @author Matthew Phillips
 */
public class ConnectionOptions
{
  private HashMap<String, Object> values;

  public ConnectionOptions ()
  {
    this.values = new HashMap<String, Object> ();
  }
  
  public void set (String name, Object value)
  {
    values.put (name, value);
  }
  
  public Object get (String name)
  {
    Object value = values.get (name);
    
    if (value == null)
      throw new IllegalArgumentException ("No value for \"" + name + "\"");
    else
      return value;
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

  public Map<String, Object> asMap ()
  {
    return unmodifiableMap (values);
  }
}
