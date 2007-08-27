package org.avis.util;

/**
 * General Avis utility functions.
 * 
 * @author Matthew Phillips
 */
public final class Util
{
  private Util ()
  {
    // zip
  }

  /**
   * Test if two objects are equal, handling null values and type differences. 
   */
  public static boolean valuesEqual (Object value1, Object value2)
  {
    if (value1 == value2)
      return true;
    else if (value1 == null || value2 == null)
      return false;
    else if (value1.getClass () == value2.getClass ())
      return value1.equals (value2);
    else
      return false;
  }

  /**
   * Check a value is non-null or throw an IllegalArgumentException.
   * 
   * @param value The value to test.
   * @param name The name of the value to be used in the exception.
   */
  public static void checkNotNull (Object value, String name)
    throws IllegalArgumentException
  {
    if (value == null)
      throw new IllegalArgumentException (name + " cannot be null");
  }
}
