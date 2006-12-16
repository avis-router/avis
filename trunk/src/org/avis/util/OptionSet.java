package org.avis.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.avis.Common.className;

/**
 * Defines the set of valid options for an {@link Options} instance.
 * An option set can inherit from one or more subsets. An option set
 * includes the valid option names, value type, valid value ranges and
 * default values.
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
    this.validation = new HashMap<String, Object> ();
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
        
        if (message != null)
          break;
      }
    }
    
    return message;
  }
  
  /**
   * Called by {@link Options#set(String, Object)} to validate and set
   * the value. If the value is valid, it should be set in the values
   * map, otherwise an IllegalOptionException should be thrown. This
   * method is also responsible for any automatic value conversion
   * (see {@link #convert(String, Object)}).
   * <p>
   * Subclasses may override to customise validation behaviour.
   * 
   * @param values The value set to update.
   * @param option The option.
   * @param value The value to validate and set.
   * 
   * @throws IllegalOptionException if the value or option are not valid.
   * 
   * @see #validate(String, Object)
   */
  protected void validateAndPut (Map<String, Object> values,
                                 String option, Object value)
    throws IllegalOptionException
  {
    value = convert (option, value);
    
    String message = validate (option, value);
    
    if (message != null)
      throw new IllegalOptionException (option, message);
    
    values.put (option, value);
  }
  
  /**
   * Check the validity of an option/value pair against this set only
   * (no inherited checks).
   * 
   * @see #validate(String, Object)
   */
  protected String testValid (String option, Object value)
    throws IllegalOptionException
  {
    Object validationInfo = validation.get (option);
    String message = null;
    
    if (validationInfo != null)
    {
      Class<?> type = optionType (option);

      if (type != value.getClass ())
      {
        message = "Value is not a " + className (type).toLowerCase ();
      } else if (value instanceof Integer)
      {
        int intValue = (Integer)value;
        int [] minMax = (int [])validation.get (option);
        
        if (!(intValue >= minMax [0] && intValue <= minMax [1]))
          message = "Value must be in range " + minMax [0] + "..." + minMax [1];
      } else if (value instanceof String)
      {
        if (!((Set)validation.get (option)).contains (value))
          message = "Value must be a string";
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
   * Integer.valueOf()) and [anything] -> String (using toString()).
   * 
   * @param option The option.
   * @param value The value.
   * @return The converted value, or the original value if no
   *         conversion needed.
   *         
   * @throws IllegalOptionException if the value needed conversion but
   *           was not compatible.
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
          value = Integer.valueOf (value.toString ());
        } catch (NumberFormatException ex)
        {
          throw new IllegalOptionException
            (option, "\"" + value + "\" is not a number");
        }
      } else
      {
        throw new IllegalOptionException
          (option, "Cannot convert " + value + " to a " +
                    className (type).toLowerCase ());
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
    Object validationInfo = findValidationInfo (option);
    
    if (validationInfo instanceof int [])
      return Integer.class;
    else if (validationInfo != null)
      return String.class;
    else
      throw new IllegalOptionException (option, "Unknown option");
  }

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
