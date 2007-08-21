package org.avis.config;

import java.util.HashSet;
import java.util.Set;

import org.avis.util.IllegalOptionException;

import static java.util.Arrays.asList;

public class OptionTypeString extends OptionType
{
  public static final OptionTypeString ANY_STRING_OPTION = new OptionTypeString ();
  
  protected Set<String> validValues;

  public OptionTypeString ()
  {
    this (null);
  }

  public OptionTypeString (String defaultValue, String... validValues)
  {
    this.validValues = new HashSet<String> (asList (validValues));
    
    this.validValues.add (defaultValue);
  }
  
  public OptionTypeString (Set<String> validValues)
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