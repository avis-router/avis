package org.avis.config;

import static org.avis.util.Text.className;

import org.avis.util.IllegalOptionException;


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

  /**
   * Utility for validate () to call if it just needs a given type.
   * 
   * @param value The value.
   * @param type The required type.
   * 
   * @return The validation response.
   */
  protected String validateType (Object value, Class<?> type)
  {
    return validateType (value, type, className (type));
  }
  
  /**
   * Utility for validate () to call if it just needs a given type.
   * 
   * @param value The value.
   * @param type The required type.
   * @param typeName A readable name for the type.
   * 
   * @return The validation response.
   */
  protected String validateType (Object value, Class<?> type,
                                  String typeName)
  {
    return type.isAssignableFrom (value.getClass ()) ? null : 
             "Value must be a " + typeName + ": " + className (value);
  }
}
