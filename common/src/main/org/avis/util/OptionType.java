package org.avis.util;

/**
 * Defines a type of option in an OptionSet.
 * 
 * @author Matthew Phillips
 */
public abstract class OptionType
{
  /**
   * Attempt to convert a value to be compatible with this option type.
   *  
   * @param option The option.
   * @param value The value.
   * @return The converted value or just value if none needed.
   * 
   * @throws IllegalOptionException if value is not convertible.
   */
  public abstract Object convert (String option, Object value)
    throws IllegalOptionException;
  
  /**
   * Check that a value is valid for this option.
   * 
   * @param option The option.
   * @param value The value.
   * 
   * @return Error text if not valid, null if OK.
   */
  public abstract String validate (String option, Object value);
}
