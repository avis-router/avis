package org.avis.config;

import java.util.HashSet;
import java.util.Set;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.avis.util.IllegalConfigOptionException;

import static org.avis.util.Text.split;
import static org.avis.util.Text.stripBackslashes;

/**
 * An option that turns space-separated items in string values into a
 * set of values by using a string constructor of a type. Backslash
 * escape can be used to quote spaces.
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
    throws IllegalConfigOptionException
  {
    try
    {
      Set<Object> values = new HashSet<Object> ();
      
      // split using regex that allows any space not preceeded by \
      for (String item : split (value.toString ().trim (), "((?<!\\\\)\\s)+"))
        values.add (constructor.newInstance (stripBackslashes (item)));
      
      return values;
    } catch (InvocationTargetException ex)
    {
      throw new IllegalConfigOptionException (option, ex.getCause ().getMessage ());
    } catch (Exception ex)
    {
      throw new IllegalConfigOptionException (option, ex.toString ());
    }
  }
}