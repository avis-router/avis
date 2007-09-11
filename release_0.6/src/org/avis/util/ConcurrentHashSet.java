package org.avis.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Concurrent hash set built on {@link ConcurrentHashMap}. Not sure
 * why Java doesn't ship with one of these already. Shamelessly
 * cribbed from JBoss's Shotoku project.
 * 
 * @author Adam Warski (adamw@aster.pl)
 */
public class ConcurrentHashSet<K> implements Set<K>
{
  private ConcurrentMap<K, Boolean> map;

  public ConcurrentHashSet ()
  {
    map = new ConcurrentHashMap<K, Boolean> ();
  }

  public int size ()
  {
    return map.size ();
  }

  public boolean isEmpty ()
  {
    return map.isEmpty ();
  }

  public boolean contains (Object o)
  {
    return map.containsKey (o);
  }

  public Iterator<K> iterator ()
  {
    return map.keySet ().iterator ();
  }

  public Object [] toArray ()
  {
    return map.keySet ().toArray ();
  }

  public <T> T [] toArray (T [] a)
  {
    return map.keySet ().toArray (a);
  }

  public boolean add (K o)
  {
    return map.putIfAbsent (o, Boolean.TRUE) == null;
  }

  public boolean remove (Object o)
  {
    return map.keySet ().remove (o);
  }

  public boolean containsAll (Collection<?> c)
  {
    return map.keySet ().containsAll (c);
  }

  public boolean addAll (Collection<? extends K> c)
  {
    boolean ret = false;
    for (K o : c)
    {
      ret |= add (o);
    }

    return ret;
  }

  public boolean retainAll (Collection<?> c)
  {
    boolean ret = false;
    for (Object o : c)
    {
      if (!map.containsKey (o))
      {
        map.remove (o);
      }
    }

    return ret;
  }

  public boolean removeAll (Collection<?> c)
  {
    boolean ret = false;
    for (Object o : c)
    {
      ret |= remove (o);
    }

    return ret;
  }

  public void clear ()
  {
    map.clear ();
  }
}
