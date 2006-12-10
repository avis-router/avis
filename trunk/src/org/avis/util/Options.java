package org.avis.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import static org.avis.util.OptionSet.EMPTY_OPTION_SET;

/**
 * Defines a set of configuration options. The options are validated
 * against an {@link OptionSet}.
 * 
 * @author Matthew Phillips
 */
public class Options
{
  private Map<String, Object> values;
  private List<Options> defaults;
  private OptionSet optionSet;
  
  public Options ()
  {
    this (EMPTY_OPTION_SET);
  }
  
  public Options (OptionSet optionSet)
  {
    this.values = new HashMap<String, Object> ();
    this.defaults = new ArrayList<Options> ();
    this.optionSet = optionSet;
    
    defaults.add (optionSet.defaults);
  }

  public void setAll (Map<String, Object> options)
  {
    for (Entry<String, Object> entry : options.entrySet ())
      set (entry.getKey (), entry.getValue ());
  }

  public void setAll (Properties properties)
  {
    for (Entry<Object, Object> entry : properties.entrySet ())
      set ((String)entry.getKey (), entry.getValue ());
  }

  public void addDefaults (Options newDefaults)
  {
    defaults.add (0, newDefaults);
  }

  public int getInt (String option)
    throws IllegalOptionException
  {
    Object value = get (option);
    
    if (value instanceof Integer)
      return (Integer)value;
    else
      throw new IllegalOptionException (option, "Not an integer");
  }
  
  public String getString (String option)
    throws IllegalOptionException
  {
    Object value = get (option);
    
    if (value instanceof String)
      return (String)value;
    else
      throw new IllegalOptionException (option, "Not a string");
  }

  public void set (String option, Object value)
    throws IllegalOptionException
  {
    if (value == null)
      throw new IllegalOptionException (option, "Value cannot be null");
    
    optionSet.validateAndPut (values, option, value);
  }
  
  public Object get (String option)
  {
    Object value = values.get (option);
    
    if (value == null)
    {
      for (Options defaultOptions : defaults)
      {
        value =  defaultOptions.get (option);
        
        if (value != null)
          break;
      }
    }
    
    return value;
  }

  public boolean hasOption (String option)
  {
    return get (option) != null;
  }

  /**
   * Return an unmodifiable, live set of the options just for this
   * instance (not including defaults).
   */
  public Set<String> options ()
  {
    return Collections.unmodifiableSet (values.keySet ());
  }
}
