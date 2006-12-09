package org.avis.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.avis.Common.className;

public class OptionSet
{
  public static final OptionSet EMPTY_OPTION_SET = new OptionSet ();

  private static final String UNKNOWN_OPTION = "Uknown option";
  
  public final Options defaults;
  
  private List<OptionSet> inherited;
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
   * Define a int-valued option.
   * 
   * @param option The option name.
   * @param min min value
   * @param defaultValue The default value
   * @param max max value
   */
  public void add (String option,
                   int min, int defaultValue, int max)
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

  public final boolean isValid (String option, Object value)
  { 
    return validate (option, value) == null; 
  }
  
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

  protected final String validate (String option, Object value)
  {
    String message = null;
    
    if (validation.containsKey (option))
    {
      message = checkValidity (option, value);
    } else
    {
      for (OptionSet inheritedSet : inherited)
      {
        message = inheritedSet.checkValidity (option, value);
        
        if (message != null)
          break;
      }
    }
    
    return message;
  }
  
  protected String checkValidity (String option, Object value)
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
      message = UNKNOWN_OPTION;
    }
    
    return message;
  }

  protected final Class<?> optionType (String option)
  {
    Object validationInfo = validation.get (option);
    
    if (validationInfo instanceof int [])
      return Integer.class;
    else if (validationInfo != null)
      return String.class;
    else
    {
      for (OptionSet inheritedSet : inherited)
      {
        Class<?> type = inheritedSet.optionType (option);
        
        if (type != null)
          return type;
      }
      
      throw new IllegalOptionException (option, "Unknown option");
    }
  }
}
