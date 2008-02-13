package org.avis.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.avis.util.IllegalConfigOptionException;

/**
 * An option type that uses the string constructor of a value type to
 * convert strings to values of that type. This can be used for any
 * option whose values can be converted from strings.
 * 
 * @author Matthew Phillips
 */
public class OptionTypeFromString extends OptionType
{
  private Class<?> valueType;
  private Constructor<?> constructor;

  public OptionTypeFromString (Class<?> valueType)
  {
    this.valueType = valueType;
    
    try
    {
      this.constructor = valueType.getConstructor (String.class);
    } catch (Exception ex)
    {
      throw new IllegalArgumentException ("No constructor taking a string");
    }
  }
    
  @Override
  public String validate (String option, Object value)
  {
    return validateType (value, valueType);
  }

  @Override
  public Object convert (String option, Object value)
    throws IllegalConfigOptionException
  {
    if (valueType.isAssignableFrom (value.getClass ()))
      return value;
    
    try
    {
      return constructor.newInstance (value.toString ());
    } catch (InvocationTargetException ex)
    {
      throw new IllegalConfigOptionException (option, 
                                              ex.getCause ().getMessage ());
    } catch (Exception ex)
    {
      throw new IllegalConfigOptionException (option, ex.toString ());
    }
  }
}
