package org.avis.util;

/**
 * General utility functions for messing with numbers.
 * 
 * @author Matthew Phillips
 */
public final class Numbers
{
  private Numbers ()
  {
    // cannot instantiate
  }
  
  /**
   * Convert a numeric value upwards to a given type.
   * 
   * @param value A numeric value.
   * @param type The target type: either Long or Double.
   * 
   * @return value upconverted to the target type.
   * 
   * @throws IllegalArgumentException if type is not valid.
   */
  public static Number upconvert (Number value, Class<?> type)
  {
    if (type == Long.class)
      return value.longValue ();
    else if (type == Double.class)
      return value.doubleValue ();
    else
      throw new IllegalArgumentException ("Cannot upconvert to " + type);
  }

  /**
   * Return the highest precision (class with the largest range) of
   * two classes.
   * 
   * @throws IllegalArgumentException if class1 or class2 is not a number.
   */
  public static Class<?>
    highestPrecision (Class<?> class1,
                      Class<?> class2)
  {
    if (precision (class1) >= precision (class2))
      return class1;
    else
      return class2;
  }

  private static int precision (Class<?> type)
  {
    if (type == Integer.class)
      return 0;
    else if (type == Long.class)
      return 1;
    else if (type == Double.class)
      return 2;
    else
      throw new IllegalArgumentException ("Unknown number type " + type);
  }
}
