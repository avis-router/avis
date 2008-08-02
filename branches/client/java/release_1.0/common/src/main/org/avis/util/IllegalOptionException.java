package org.avis.util;

/**
 * Thrown when an illegal option name or value is used in a
 * configuration or command line setup.
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
