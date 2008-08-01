package org.avis.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

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
   * Create an immutable set from a number of items.
   */
  public static <E> Set<E> set (E... items)
  {
    return unmodifiableSet (new HashSet<E> (asList (items)));
  }
  
  /**
   * Create an immutable list from a number of items.
   */
  public static <E> List<E> list (E... items)
  {
    return unmodifiableList (asList (items));
  }
  
  /**
   * Create an immutable map from a number of item pairs: even items
   * are keys, their adjacent items are values.
   */
  public static <E> Map<E, E> map (E... pairs)
  {
    if (pairs.length % 2 != 0)
      throw new IllegalArgumentException ("Items must be a set of pairs");
    
    HashMap<E, E> map = new HashMap<E, E> ();
    
    for (int i = 0; i < pairs.length; i += 2)
      map.put (pairs [i], pairs [i + 1]);

    return unmodifiableMap (map);
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

  /**
   * Compute the difference between set1 and set2.
   */
  public static <E> Set<E> difference (Set<E> set1, Set<E> set2)
  {
    if (set1.isEmpty () || set2.isEmpty ())
      return set1;

    Set<E> diff = new HashSet<E> ();
    
    for (E item : set1)
    {
      if (!set2.contains (item))
        diff.add (item);
    }
    
    return diff;
  }
}