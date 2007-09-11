package org.avis.net.security;

import java.util.HashSet;
import java.util.Set;

/**
 * A single set of keys. Can be used directly as a java.util.Set.
 *  
 * @author Matthew Phillips
 */
public class SingleKeySet extends HashSet<Key> implements KeySet, Set<Key>
{
  public void add (KeySet theKeys)
    throws IllegalArgumentException
  {
    addAll ((SingleKeySet)theKeys);
  }

  public void remove (KeySet theKeys)
  {
    removeAll ((SingleKeySet)theKeys);
  }

  public boolean remove (Key key)
  {
    return remove ((Object)key);
  }
}
