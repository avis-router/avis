package org.avis.net.security;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

import static org.avis.net.security.DualKeyScheme.PRODUCER;
import static org.avis.net.security.DualKeyScheme.checkProdOrCon;
import static org.avis.util.Collections.difference;

/**
 * A pair of key sets (producer/consumer) used for dual key schemes.
 * 
 * @see Keys
 * @see DualKeyScheme
 * 
 * @author Matthew Phillips
 */
public final class DualKeySet implements KeySet
{
  public final Set<Key> producerKeys;
  public final Set<Key> consumerKeys;
  
  DualKeySet ()
  {
    this.producerKeys = new HashSet<Key> ();
    this.consumerKeys = new HashSet<Key> ();
  }
 
  /**
   * Create an immutable empty instance.
   */
  DualKeySet (boolean immutable)
  {
    this.producerKeys = emptySet ();
    this.consumerKeys = emptySet ();
  }

  DualKeySet (Set<Key> producerKeys, Set<Key> consumerKeys)
  {
    this.producerKeys = producerKeys;
    this.consumerKeys = consumerKeys;
  }

  /**
   * Get the keys for a producer or consumer.
   * 
   * @param prodOrCon One of {@link DualKeyScheme#PRODUCER} or
   *          {@link DualKeyScheme#CONSUMER}.
   */           
  public Set<Key> keysFor (int prodOrCon)
  {
    checkProdOrCon (prodOrCon);
    
    if (prodOrCon == PRODUCER)
      return producerKeys;
    else
      return consumerKeys;
  }
  
  public boolean isEmpty ()
  {
    return producerKeys.isEmpty () && consumerKeys.isEmpty ();
  }
  
  public int size ()
  {
    return producerKeys.size () + consumerKeys.size ();
  }
  
  public void add (KeySet theKeys)
    throws IllegalArgumentException
  {
    DualKeySet keys = (DualKeySet)theKeys;
    
    producerKeys.addAll (keys.producerKeys);
    consumerKeys.addAll (keys.consumerKeys);
  }
  
  public void remove (KeySet theKeys)
    throws IllegalArgumentException
  {
    DualKeySet keys = (DualKeySet)theKeys;
    
    producerKeys.removeAll (keys.producerKeys);
    consumerKeys.removeAll (keys.consumerKeys);
  }
  
  public boolean add (Key key)
    throws IllegalArgumentException, UnsupportedOperationException
  {
    throw new UnsupportedOperationException ("Cannot add to a dual key set");
  }
  
  public boolean remove (Key key)
    throws IllegalArgumentException, UnsupportedOperationException
  {
    throw new UnsupportedOperationException ("Cannot remove from a dual key set");
  }
  
  public KeySet subtract (KeySet theKeys)
  {
    DualKeySet keys = (DualKeySet)theKeys;
    
    return new DualKeySet (difference (producerKeys, keys.producerKeys),
                           difference (consumerKeys, keys.consumerKeys));
  }
  
  @Override
  public boolean equals (Object object)
  {
    return object instanceof DualKeySet && equals ((DualKeySet)object);
  }
  
  public boolean equals (DualKeySet keyset)
  {
    return producerKeys.equals (keyset.producerKeys) &&
           consumerKeys.equals (keyset.consumerKeys);
  }
  
  @Override
  public int hashCode ()
  {
    return producerKeys.hashCode () ^ consumerKeys.hashCode ();
  }
}
