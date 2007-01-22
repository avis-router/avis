package org.avis.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
  
  /**
   * Create a list from a number of strings.
   */
  public static List<String> list (String... strings)
  {
    return new ArrayList<String> (Arrays.asList (strings));
  }
}
