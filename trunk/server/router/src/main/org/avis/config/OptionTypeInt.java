package org.avis.config;

import org.avis.util.IllegalConfigOptionException;

import static java.lang.Integer.parseInt;

import static org.avis.common.Common.K;
import static org.avis.common.Common.MB;

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
        unit = MB;
        text = text.substring (0, text.length () - 1);
      } else if (text.endsWith ("k"))
      {
        unit = K;
        text = text.substring (0, text.length () - 1);
      }

      return parseInt (text) * unit;
    } catch (NumberFormatException ex)
    {
      throw new IllegalConfigOptionException
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