package org.avis.config;


/**
 * Thrown when an illegal option name or value is used with
 * {@link Options} or {@link OptionSet}.
 * 
 * @author Matthew Phillips
 */
public class IllegalOptionException extends IllegalArgumentException
{
  public IllegalOptionException (String message)
  {
    super (message);
  }

  public IllegalOptionException (String option, String message)
  {
    this ("Error in option \"" + option + "\": " + message);
  }
}
