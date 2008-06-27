package org.avis.config;

import org.avis.util.IllegalOptionException;


public class OptionTypeInt extends OptionType
{
  protected int min;
  protected int max;

  public OptionTypeInt (int min, int max)
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