package org.avis.config;

import org.avis.util.IllegalOptionException;


public class OptionTypeBoolean extends OptionType
{
  public static final OptionTypeBoolean INSTANCE = new OptionTypeBoolean ();

  @Override
  public Object convert (String option, Object value)
    throws IllegalOptionException
  {
    if (value instanceof Boolean)
      return value;
    
    String v = value.toString ().trim ().toLowerCase ();
    
    if (v.equals ("1") || v.equals ("true") || v.equals ("yes"))
      return true;
    else if (v.equals ("0") || v.equals ("false") || v.equals ("no"))
      return false;
    else
      throw new IllegalOptionException
        (option, "\"" + value + "\" is not a valid true/false boolean");
  }
  
  @Override
  public String validate (String option, Object value)
  {
    return value instanceof Boolean ? null : "Value must be true/false";
  }
}