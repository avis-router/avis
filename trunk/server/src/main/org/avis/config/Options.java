package org.avis.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.avis.util.IllegalOptionException;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

import static org.avis.config.OptionSet.EMPTY_OPTION_SET;

/**
 * Defines a set of configuration options. The options are validated
 * against an {@link OptionSet}. Default values are taken from the
 * option set, but may be overridden/added in this instance using
 * {@link #addDefaults(Options)}.
 * 
 * @author Matthew Phillips
 */
public class Options implements Iterable<Map.Entry<String, Object>>
{
  protected Map<String, Object> values;
  protected List<Options> defaults;
  protected OptionSet optionSet;
  
  public Options ()
  {
    this (EMPTY_OPTION_SET);
  }
  
  public Options (OptionSet optionSet)
  {
    this.values = new TreeMap<String, Object> (CASE_INSENSITIVE_ORDER);
    this.defaults = new ArrayList<Options> ();
    this.optionSet = optionSet;
  }
  
  public OptionSet optionSet ()
  {
    return optionSet;
  }

  public Iterator<Entry<String, Object>> iterator ()
  {
    return values.entrySet ().iterator ();
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
   * Get a boolean option.
   * 
   * @param option The option name.
   * @return The value.
   * 
   * @throws IllegalOptionException if the option is not defined or is
   *           not a boolean.
   * 
   * @see #get(String)
   */
  public boolean getBoolean (String option)
    throws IllegalOptionException
  {
    Object value = get (option);
    
    if (value instanceof Boolean)
      return (Boolean)value;
    else
      throw new IllegalOptionException (option, "Not a boolean");
  }
  
  /**
   * Get a value for a parameterised option. e.g.
   * "Federation.Subscribe[Internal]".
   * 
   * @param option The option, minus the parameters.
   * 
   * @return The value of the option, mapping parameters to values.
   */
  public Map<String, Object> getParamOption (String option)
  {
    return OptionTypeParam.getParamOption (this, option);
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
      throw new IllegalOptionException (option, "Undefined option");
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
    
    // look in defaults on this option set
    if (value == null)
    {
      for (Options options : defaults)
      {
        value = options.peek (option);
        
        if (value != null)
          break;
      }
    }
    
    // look in option set for defaults
    if (value == null)
      value = optionSet.peekDefaultValue (option);
    
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
   * @see OptionSet#validateAndSet(Options, String, Object)
   */
  public void set (String option, Object value)
    throws IllegalOptionException
  {
    if (value == null)
      throw new IllegalOptionException (option, "Value cannot be null");
    
    OptionSet set = optionSet.findOptionSetFor (option);
    
    /*
     * If no option set found, fall back on this one in case
     * validateAndSet () can do something clever
     */
    if (set == null)
      set = optionSet;
    
    set.validateAndSet (this, option, value);
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
    return unmodifiableSet (values.keySet ());
  }
  
  /**
   * Return an unmodifiable, live map containing all the options and
   * values in this instance (not including defaults).
   */
  public Map<String, Object> asMap ()
  {
    return unmodifiableMap (values);
  }
}
