package org.avis.config;

import java.util.HashSet;
import java.util.Set;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.avis.util.IllegalOptionException;

import static org.avis.util.Text.split;

/**
 * An option that turns space-separated items in string values into
 * a set of values by using a string constructor of a type.
 */
public class OptionTypeSet extends OptionType
{
  private Constructor<?> constructor;

  public OptionTypeSet (Class<?> setValueType)
  {
    try
    {
      this.constructor = setValueType.getConstructor (String.class);
    } catch (Exception ex)
    {
      throw new IllegalArgumentException ("No constructor taking a string");
    }
  }
  
  @Override
  public String validate (String option, Object value)
  {
    return validateType (value, Set.class);
  }
  
  @Override
  public Object convert (String option, Object value)
    throws IllegalOptionException
  {
    try
    {
      Set<Object> values = new HashSet<Object> ();
      
      for (String item : split (value.toString (), "\\s+"))
        values.add (constructor.newInstance (item));
      
      return values;
    } catch (InvocationTargetException ex)
    {
      throw new IllegalOptionException (option, ex.getCause ().getMessage ());
    } catch (Exception ex)
    {
      throw new IllegalOptionException (option, ex.toString ());
    }
  }
}