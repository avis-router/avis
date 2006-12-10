package org.avis.util;

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
    this (message + ": " + option);
  }
}
