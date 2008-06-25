package org.avis.config;

import org.avis.util.IllegalConfigOptionException;
import org.avis.util.InvalidFormatException;
import org.avis.util.Text;

import static org.avis.util.Text.stringToValue;

/**
 * An option that uses {@link Text#stringToValue(String)} to convert
 * its value.
 * 
 * @author Matthew Phillips
 */
public class OptionTypeValueExpr extends OptionType
{
  @Override
  public String validate (String option, Object value)
  {
    return null;
  }

  @Override
  public Object convert (String option, Object value)
    throws IllegalConfigOptionException
  {
    try
    {
      return stringToValue (value.toString ());
    } catch (InvalidFormatException ex)
    {
      throw new IllegalConfigOptionException (option, ex.getMessage ());
    }
  }
}
