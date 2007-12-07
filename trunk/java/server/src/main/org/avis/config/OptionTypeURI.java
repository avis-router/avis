package org.avis.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.avis.util.IllegalOptionException;

/**
 * A URI-valued option.
 * 
 * @author Matthew Phillips
 */
public class OptionTypeURI extends OptionType
{
  @Override
  public Object convert (String option, Object value)
    throws IllegalOptionException
  {
    try
    {
      if (value instanceof URI)
        return value;
      else if (value instanceof URL)
        return ((URL)value).toURI ();
      else
        return new URI (value.toString ());
    } catch (URISyntaxException ex)
    {
      throw new IllegalOptionException
        (option, "\"" + value + "\" is not a valid URI");
    }
  }

  @Override
  public String validate (String option, Object value)
  {
    if (value instanceof URI)
      return null;
    else
      return "Value is not a URI";
  }
}
