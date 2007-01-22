package org.avis.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
  
  /**
   * Create a map from a number of strings, even items are keys,
   * adjacent items are values.
   */
  public static Map<String, String> map (String... strings)
  {
    if (strings.length % 2 != 0)
      throw new IllegalArgumentException ("Strings must be a set of pairs");
    
    HashMap<String, String> map = new HashMap<String, String> ();
    
    for (int i = 0; i < strings.length; i += 2)
      map.put (strings [i], strings [i + 1]);

    return map;
  }

  /**
   * Join a collection of strings with a separator and append to a
   * string builder.
   */
  public static void join (StringBuilder str,
                           Collection<String> strings,
                           char separator)
  {
    boolean first = true;

    for (String string : strings)
    {
      if (!first)
        str.append (separator);
      
      first = false;
      
      str.append (string);
    }
  }
}
