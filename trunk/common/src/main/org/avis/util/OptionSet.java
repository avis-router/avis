package org.avis.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Arrays.asList;

/**
 * Defines the set of valid options for an {@link Options} instance.
 * An option set can inherit from one or more subsets. An option set
 * includes the valid option names, value type, valid value ranges and
 * default values. Option names are stored in a case-preserving
 * manner, but are matched case-insensitively.
 * 
 * @author Matthew Phillips
 */
public class OptionSet
{
  public static final OptionSet EMPTY_OPTION_SET = new OptionSet ();

  /** The default values for each option. */
  public final Options defaults;
  
  /** The inherited sets. */
  private List<OptionSet> inherited;
  /** Maps option names to validation info. */
  private Map<String, OptionType> validation;
  
  public OptionSet ()
  {
    this.defaults = new Options (this);
    this.inherited = new ArrayList<OptionSet> ();
    this.validation = new TreeMap<String, OptionType> (CASE_INSENSITIVE_ORDER);
  }
  
  public OptionSet (OptionSet inheritedOptions)
  {
    this ();
    
    inherited.add (inheritedOptions);
  }
  
  /**
   * Inherit from a given option set.
   */
  public void inheritFrom (OptionSet optionSet)
  {
    inherited.add (optionSet);
  }
  
  /**
   * Look for the default value specified by this option set or any of
   * its inherited sets.
   * 
   * @param option The option to find a value for.
   * 
   * @return The value, or null if none found.
   */
  public Object peekDefaultValue (String option)
  {
    Object value = defaults.values.get (option);
    
    if (value == null)
    {
      for (OptionSet superSet : inherited)
      {
        value = superSet.defaults.values.get (option);
        
        if (value != null)
          break;
      }
    }
    
    return value;
  }
  
  /**
   * Define an int-valued option.
   * 
   * @param option The option name.
   * @param min The minimum value
   * @param defaultValue The default value
   * @param max The maximum value
   */
  public void add (String option, int min, int defaultValue, int max)
  {
    add (option, new IntOption (min, max), defaultValue);
  }
  
  public void add (String option, boolean defaultValue)
  {
    add (option, BooleanOption.INSTANCE, defaultValue);
  }
  
  /**
   * Define a string-valued option that can take any value.
   * 
   * @param option The option name.
   * @param defaultValue The default value.
   */
  public void add (String option, String defaultValue)
  {
    add (option, new StringOption (), defaultValue);
  }
  
  /**
   * Define a string-valued option.
   * 
   * @param option The option name.
   * @param defaultValue The default value.
   * @param values Valid values (other than default).
   */
  public void add (String option, String defaultValue,
                   String... values)
  {
    add (option, new StringOption (defaultValue, values), defaultValue);
  }
  
  /**
   * Add an option with a default value.
   */
  protected void add (String option, OptionType type, Object defaultValue)
  {
    validation.put (option, type);
    set (defaults, option, defaultValue);
  }

  /**
   * Test if value is valid for a given option (does not set the value).
   * 
   * @see #validate(String, Object)
   */
  public final boolean isValid (String option, Object value)
  { 
    return validate (option, value) == null; 
  }
  
  /**
   * Test if a given option name is defined by this set or a subset.
   */
  public boolean isDefined (String option)
  {
    return findOptionType (option) != null;
  }
  
  /**
   * Get the maximum value for an int option.
   */
  public int getMaxValue (String name)
    throws IllegalOptionException
  {
    return intOption (name).max;
  }
  
  /**
   * Get the minimum value for an int option.
   */
  public int getMinValue (String name)
    throws IllegalOptionException
  {
    return intOption (name).min;
  }
  
  private IntOption intOption (String name)
    throws IllegalOptionException
  {
    OptionType info = findOptionType (name);
    
    if (info instanceof IntOption)
      return (IntOption)info;
    else
      throw new IllegalOptionException (name, "Not an integer value");  
  }
  
  /**
   * Test if value is valid for a given option in this set or any
   * inherited sets. (does not set the value).
   * 
   * @return Null if valid, a message describing why the value is
   *         invalid otherwise.
   */
  public final String validate (String option, Object value)
  {
    String message = null;
    
    if (validation.containsKey (option))
    {
      message = testValid (option, value);
    } else
    {
      for (OptionSet inheritedSet : inherited)
      {
        message = inheritedSet.testValid (option, value);
        
        // if one inherited set accepts the option, we're done
        if (message == null)
          break;
      }
    }
    
    return message;
  }
  
  /**
   * Called by {@link Options#set(String, Object)} to validate and set
   * the value. If the value is valid, it should be set with a call to
   * {@link #set(Options, String, Object)}, otherwise an
   * IllegalOptionException should be thrown. This method is also
   * responsible for any automatic value conversion (see
   * {@link OptionType#convert(String, Object)}).
   * <p>
   * Subclasses may override to customise validation behaviour.
   * 
   * @param options The options to update.
   * @param option The option.
   * @param value The value to validate and set.
   * 
   * @throws IllegalOptionException if the value or option are not
   *           valid.
   * 
   * @see #validate(String, Object)
   */
  protected void validateAndSet (Options options, String option, Object value)
  {
    OptionType type = optionTypeFor (option);
    
    value = type.convert (option, value);
    
    String message = type.validate (option, value);
    
    if (message == null)
      set (options, option, value);
    else
      throw new IllegalOptionException (option, message);
  }
  
  /**
   * Set a value in the options with no validation. This is usually
   * called to set validated values from
   * {@link #validateAndSet(Options, String, Object)}.
   */
  protected final void set (Options options, String option, Object value)
  {
    options.values.put (option, value);
  }

  /**
   * Check the validity of an option/value pair against this set only
   * (no inherited checks).
   * 
   * @see #validate(String, Object)
   */
  private String testValid (String option, Object value)
  {
    return optionTypeFor (option).validate (option, value);
  }
  
  /**
   * Shortcut to {@link OptionType#convert(String, Object)}.
   * 
   * @throws IllegalOptionException if option not defined or value is
   *                 invalid.
   */
  public Object convert (String option, Object value)
    throws IllegalOptionException
  {
    return optionTypeFor (option).convert (option, value);
  }
  
  /**
   * Get the option type for a given option.
   * 
   * @throws IllegalOptionException if option is not defined.
   */
  public OptionType optionTypeFor (String option)
    throws IllegalOptionException
  {
    OptionType type = findOptionType (option);
    
    if (type == null)
      throw new IllegalOptionException (option, "Undefined option");
    
    return type;
  }
  /**
   * Recursively search this set and subsets for an option's type.
   * 
   * @param option Name of option. Must be lower case.
   */
  private OptionType findOptionType (String option)
  {
    OptionType optionType = validation.get (option);
    
    if (optionType == null)
    {
      for (OptionSet inheritedSet : inherited)
      {
        optionType = inheritedSet.findOptionType (option);
        
        if (optionType != null)
          break;
      }
    }
    
    return optionType;
  }
  
  public static class IntOption extends OptionType
  {
    protected int min;
    protected int max;

    public IntOption (int min, int max)
    {
      this.min = min;
      this.max = max;
    }
    
    @Override
    public Object convert (String option, Object value)
    {
      if (value instanceof Integer)
        return value;
      
      try
      {
        String text = value.toString ().toLowerCase ();
        int unit = 1;
        
        if (text.endsWith ("m"))
        {
          unit = 1024*1024;
          text = text.substring (0, text.length () - 1);
        } else if (text.endsWith ("k"))
        {
          unit = 1024;
          text = text.substring (0, text.length () - 1);
        }
        
        return Integer.parseInt (text) * unit;
      } catch (NumberFormatException ex)
      {
        throw new IllegalOptionException
          (option, "\"" + value + "\" is not a valid integer");
      }
    }
    
    @Override
    public String validate (String option, Object value)
    {
      if (value instanceof Integer)
      {
        int intValue = (Integer)value;
        
        if (intValue >= min && intValue <= max)
          return null;
        else
          return "Value must be in range " + min + ".." + max;
      } else
      {
        return "Value is not an integer";
      }
    }
  }
  
  public static class BooleanOption extends OptionType
  {
    public static final BooleanOption INSTANCE = new BooleanOption ();

    @Override
    public Object convert (String option, Object value)
      throws IllegalOptionException
    {
      if (value instanceof Boolean)
        return value;
      
      String v = value.toString ().trim ().toLowerCase ();
      
      if (v.equals ("1") || v.equals ("true") || v.equals ("yes"))
        return true;
      else if (v.equals ("0") || v.equals ("false") || v.equals ("no"))
        return false;
      else
        throw new IllegalOptionException
          (option, "\"" + value + "\" is not a valid true/false boolean");
    }
    
    @Override
    public String validate (String option, Object value)
    {
      return value instanceof Boolean ? null : "Value must be true/false";
    }
  }

  public static class StringOption extends OptionType
  {
    protected Set<String> validValues;

    public StringOption ()
    {
      this (null);
    }

    public StringOption (String defaultValue, String... validValues)
    {
      this.validValues = new HashSet<String> (asList (validValues));
      
      this.validValues.add (defaultValue);
    }
    
    public StringOption (Set<String> validValues)
    {
      this.validValues = validValues;
    }
    
    @Override
    public Object convert (String option, Object value)
      throws IllegalOptionException
    {
      return value.toString ();
    }
    
    @Override
    public String validate (String option, Object value)
    {
      if (value instanceof String)
      {
        if (validValues != null && !validValues.contains (value))
          return "Value must be one of: " + validValues.toString ();
        else
          return null;
      } else
      {
        return "Value must be a string";
      }
    }
  }
}
