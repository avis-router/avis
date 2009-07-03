package org.avis.config;

import java.net.MalformedURLException;
import java.net.URL;

import org.avis.util.IllegalConfigOptionException;

/**
 * A HTTP URL option.
 * 
 * @author Matthew Phillips
 */
public class OptionTypeHttpUrl extends OptionType
{
  @Override
  public Object convert (String option, Object value)
    throws IllegalConfigOptionException
  {
    try
    {
      if (value instanceof URL)
        return value;
      else
        return new URL (value.toString ());
    } catch (MalformedURLException ex)
    {
      throw new IllegalConfigOptionException
        (option, "\"" + value + "\" is not a valid HTTP URL");
    }
  }

  @Override
  public String validate (String option, Object value)
  {
    String protocol = "";
    
    if (value instanceof URL)
      protocol = ((URL)value).getProtocol ();

    if (protocol.equals ("http") || protocol.equals ("https"))
      return null;
    else
      return "Must be a HTTP or HTTPS URL";
  }
}
