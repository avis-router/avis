package org.avis.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * General collection utilities.
 * 
 * @author Matthew Phillips
 */
public class Collections
{
  /**
   * Create a set from a number of strings.
   */
  public static Set<String> set (String... strings)
  {
    return new HashSet<String> (Arrays.asList (strings));
  }
}
