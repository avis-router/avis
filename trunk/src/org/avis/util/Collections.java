package org.avis.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * General collection utilities.
 * 
 * @author Matthew Phillips
 */
public final class Collections
{
  private Collections ()
  {
    // cannot instantiate
  }
  
  /**
   * Create a set from a number of items.
   */
  public static <E> Set<E> set (E... items)
  {
    return new HashSet<E> (asList (items));
  }
  
  /**
   * Create a list from a number of items.
   */
  public static <E> List<E> list (E... items)
  {
    return new ArrayList<E> (asList (items));
  }
  
  /**
   * Create a map from a number of item pairs, even items are keys,
   * adjacent items are values.
   */
  public static <E> Map<E, E> map (E... pairs)
  {
    if (pairs.length % 2 != 0)
      throw new IllegalArgumentException ("Items must be a set of pairs");
    
    HashMap<E, E> map = new HashMap<E, E> ();
    
    for (int i = 0; i < pairs.length; i += 2)
      map.put (pairs [i], pairs [i + 1]);

    return map;
  }

  /**
   * Join a collection of items with a separator and append to a
   * string builder.
   */
  public static void join (StringBuilder str,
                           Iterable<?> items,
                           char separator)
  {
    boolean first = true;

    for (Object item : items)
    {
      if (!first)
        str.append (separator);
      
      first = false;
      
      str.append (item);
    }
  }
}
