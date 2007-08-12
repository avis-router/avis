package org.avis.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Collections.emptySet;

import static org.avis.util.Text.className;

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
  private Map<String, Object> validation;
  
  public OptionSet ()
  {
    this.defaults = new Options (this);
    this.inherited = new ArrayList<OptionSet> ();
    this.validation = new TreeMap<String, Object> (CASE_INSENSITIVE_ORDER);
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
    validation.put (option, new int [] {min, max});
    defaults.set (option, defaultValue);
  }
  
  public void add (String option, boolean defaultValue)
  {
    validation.put (option, Boolean.class);
    defaults.set (option, defaultValue);
  }
  
  /**
   * Define a string-valued option that can take any value.
   * 
   * @param option The option name.
   * @param defaultValue The default value.
   */
  public void add (String option, String defaultValue)
  {
    validation.put (option, emptySet ());
    defaults.set (option, defaultValue);
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
    HashSet<String> valueSet = new HashSet<String> (Arrays.asList (values));
    valueSet.add (defaultValue);
  
    validation.put (option, valueSet);
    defaults.set (option, defaultValue);
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
    return findValidationInfo (option) != null;
  }
  
  /**
   * Get the maximum value for an option.
   */
  public int getMaxValue (String name)
    throws IllegalOptionException
  {
    return intRange (name) [1];
  }
  
  /**
   * Get the minimum value for an option.
   */
  public int getMinValue (String name)
    throws IllegalOptionException
  {
    return intRange (name) [0];
  }
  
  private int [] intRange (String name)
    throws IllegalOptionException
  {
    Object info = findValidationInfo (name);
    
    if (info instanceof int [])
      return (int [])info;
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
   * {@link #convert(String, Object)}).
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
  protected void validateAndPut (Options options,
                                 String option, Object value)
    throws IllegalOptionException
  {
    value = convert (option, value);
    
    String message = validate (option, value);
    
    if (message != null)
      throw new IllegalOptionException (option, message);
    
    set (options, option, value);
  }
  
  /**
   * Called by {@link #validateAndPut(Options, String, Object)} to set
   * a value.
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
  protected String testValid (String option, Object value)
  {
    Object validationInfo = validation.get (option);
    String message = null;
    
    if (validationInfo != null)
    {
      Class<?> type = optionType (validationInfo);

      if (type != value.getClass ())
      {
        message = "Value is not a " + className (type);
      } else if (value instanceof Integer)
      {
        int intValue = (Integer)value;
        int [] minMax = (int [])validationInfo;
        
        if (!(intValue >= minMax [0] && intValue <= minMax [1]))
          message = "Value must be in range " + minMax [0] + ".." + minMax [1];
      } else if (value instanceof String)
      {
        Set<?> values = (Set<?>)validationInfo;
        
        if (!values.isEmpty () && !values.contains (value))
          message = "Value must be one of: " + values.toString ();
      } else if (value instanceof Boolean)
      {
        return null;
      } else
      {
        // should not be able to get here if options defined correctly
        throw new Error ();
      }
    } else
    {
      message = "Unknown option";
    }
    
    return message;
  }
  
  /**
   * Try to auto-convert a value to be the valid type for a given
   * option. Currently tries to convert String -> int (using
   * Integer.valueOf()), [anything] -> String (using toString()), and
   * true/false/yes/no to boolean.
   * 
   * @param option The option.
   * @param value The value.
   * @return The converted value, or the original value if no
   *         conversion needed.
   * 
   * @throws IllegalOptionException if the value needed conversion but
   *                 was not compatible.
   */
  protected final Object convert (String option, Object value)
    throws IllegalOptionException
  {
    if (value == null)
      return value;
    
    Class<?> type = optionType (option);
    
    if (type != value.getClass ())
    {
      if (type == String.class)
      {
        value = value.toString ();
      } else if (type == Integer.class)
      {
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
          
          value = Integer.parseInt (text) * unit;
        } catch (NumberFormatException ex)
        {
          throw new IllegalOptionException
            (option, "\"" + value + "\" is not a valid 32-bit integer");
        }
      } else if (type == Boolean.class)
      {
        String v = value.toString ().trim ().toLowerCase ();
        
        if (v.equals ("1") || v.equals ("true") || v.equals ("yes"))
          value = true;
        else if (v.equals ("0") || v.equals ("false") || v.equals ("no"))
          value = false;
        else
          throw new IllegalOptionException
            (option, "\"" + value + "\" is not a valid true/false boolean");
      } else
      {
        throw new IllegalOptionException
          (option, "Cannot convert " + value + " to a " + className (type));
      }
    }
    
    return value;
  }

  /**
   * Get the type of value required by an option, including inherited
   * options.
   * 
   * @throws IllegalOptionException if the option is not defined.
   */
  protected final Class<?> optionType (String option)
    throws IllegalOptionException
  {
    Class<?> type = optionType (findValidationInfo (option));
    
    if (type !=  null)
      return type;
    else
      throw new IllegalOptionException (option, "Unknown option");
  }
  
  /**
   * Get the type of value required by an option using supplied
   * validation info.
   * 
   * @throws IllegalOptionException if the option is not defined.
   */
  protected final Class<?> optionType (Object validationInfo)
    throws IllegalOptionException
  {
    if (validationInfo instanceof int [])
      return Integer.class;
    else if (validationInfo == Boolean.class)
      return Boolean.class;
    else if (validationInfo != null)
      return String.class;
    else
      return null;
  }

  /**
   * Recursively search this set and subsets for validation info.
   * 
   * @param option Name of option. Must be lower case.
   */
  private Object findValidationInfo (String option)
  {
    Object validationInfo = validation.get (option);
    
    if (validationInfo == null)
    {
      for (OptionSet inheritedSet : inherited)
      {
        validationInfo = inheritedSet.findValidationInfo (option);
        
        if (validationInfo != null)
          break;
      }
    }
    
    return validationInfo;
  }
}
