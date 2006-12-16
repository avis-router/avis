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
 * against an {@link OptionSet}. Default values are taken from the
 * option set, but may be overridden/added in this instance using
 * {@link #addDefaults(Options)}.
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
    
    if (optionSet.defaults != null)
      defaults.add (optionSet.defaults);
  }
  
  /**
   * Add a set of options as defaults. Any overlapping values override
   * existing option defaults in this set and the validating option
   * set.
   */
  public void addDefaults (Options newDefaults)
  {
    defaults.add (0, newDefaults);
  }

  /**
   * Set a bulk lot of options. This is equivalent to calling
   * {@link #set(String, Object)} for each entry.
   * 
   * @throws IllegalOptionException
   */
  public void setAll (Map<String, Object> options)
    throws IllegalOptionException
  {
    for (Entry<String, Object> entry : options.entrySet ())
      set (entry.getKey (), entry.getValue ());
  }

  /**
   * Set a bulk lot of options from java.util.Properties source.
   * 
   * @throws IllegalOptionException
   */
  public void setAll (Properties properties)
    throws IllegalOptionException
  {
    for (Entry<Object, Object> entry : properties.entrySet ())
      set ((String)entry.getKey (), entry.getValue ());
  }

  /**
   * Get an integer option.
   * 
   * @param option The option name.
   * @return The value.
   * 
   * @throws IllegalOptionException if the option is not defined or is
   *           not an integer.
   *           
   * @see #get(String)
   */
  public int getInt (String option)
    throws IllegalOptionException
  {
    Object value = get (option);
    
    if (value instanceof Integer)
      return (Integer)value;
    else
      throw new IllegalOptionException (option, "Not an integer");
  }
  
  /**
   * Get a string option.
   * 
   * @param option The option name.
   * @return The value.
   * 
   * @throws IllegalOptionException if the option is not defined or is
   *           not a string.
   *           
   * @see #get(String)
   */
  public String getString (String option)
    throws IllegalOptionException
  {
    Object value = get (option);
    
    if (value instanceof String)
      return (String)value;
    else
      throw new IllegalOptionException (option, "Not a string");
  }

  /**
   * Get the value of an option, searching defaults if needed.
   * 
   * @param option The option name
   * @return The value.
   * 
   * @throws IllegalOptionException if the option is not defined.
   * 
   * @see #peek(String)
   * @see #set(String, Object)
   * @see #isDefined(String)
   */
  public Object get (String option)
    throws IllegalOptionException
  {
    Object value = peek (option);
    
    if (value != null)
      return value;
    else
      throw new IllegalOptionException (option, "No such option");
  }
  
  /**
   * Same as get(), but returns null if no value found rather than
   * throwing an exception.
   * 
   * @see #get(String)
   */
  public Object peek (String option)
  {
    Object value = values.get (option);
    
    if (value == null)
    {
      for (Options options : defaults)
      {
        value = options.peek (option);
        
        if (value != null)
          break;
      }
    }
    
    return value;
  }

  /**
   * Set the value of an option.
   * 
   * @param option The option name.
   * @param value The option value.
   * 
   * @throws IllegalOptionException if the option is not defined or
   *           the value is invalid.
   *           
   * @see #get(String)
   * @see #remove(String)
   * @see OptionSet#validateAndPut(Map, String, Object)
   */
  public void set (String option, Object value)
    throws IllegalOptionException
  {
    if (value == null)
      throw new IllegalOptionException (option, "Value cannot be null");
    
    optionSet.validateAndPut (values, option, value);
  }

  /**
   * Undo the effect of set ().
   * 
   * @param option The option to remove.
   * 
   * @see #set(String, Object)
   */
  public void remove (String option)
  {
    values.remove (option);
  }
  
  /**
   * Test if an option is defined.
   */
  public boolean isDefined (String option)
  {
    return peek (option) != null;
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
