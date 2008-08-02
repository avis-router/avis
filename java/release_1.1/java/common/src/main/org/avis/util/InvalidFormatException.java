package org.avis.util;

/**
 * Thrown when a parsing process detects some sort of invalid format
 * in its input.
 * 
 * @author Matthew Phillips
 */
public class InvalidFormatException extends Exception
{
  public InvalidFormatException (String message)
  {
    super (message);
  }
}
