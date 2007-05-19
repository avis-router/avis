package org.avis.security;

import java.util.Set;

import static org.avis.security.SecureHash.SHA1;
import static org.avis.util.Collections.set;

/**
 * An enumeration of supported security key schemes e.g. SHA-1 Dual,
 * SHA-1 Consumer, etc.
 * 
 * @author Matthew Phillips
 */
public abstract class KeyScheme
{  
  public static final DualKeyScheme SHA1_DUAL =
    new DualKeyScheme (1, SHA1);
  
  public static final SingleKeyScheme SHA1_PRODUCER =
    new SingleKeyScheme (2, SHA1, true, false);
  
  public static final SingleKeyScheme SHA1_CONSUMER =
    new SingleKeyScheme (3, SHA1, false, true);

  private static final Set<KeyScheme> SCHEMES =
    set (SHA1_CONSUMER, SHA1_PRODUCER, SHA1_DUAL);
  
  /**
   * The ID of the scheme. Same as the on-the-wire value.
   */
  public final int id;
  public final boolean producer;
  public final boolean consumer;
  public final SecureHash keyHash;
  public final String name;

  KeyScheme (int id, SecureHash keyHash, boolean producer, boolean consumer)
  {
    this.id = id;
    this.producer = producer;
    this.consumer = consumer;
    this.keyHash = keyHash;
    this.name = createName ();
  }
  
  /**
   * True if the scheme requires dual key sets.
   */
  public boolean isDual ()
  {
    return producer && consumer;
  }
  
  /**
   * Create the public (aka prime) key for a given private (aka raw)
   * key using this scheme's hash.
   * 
   * todo opt: cache key hashes?
   */
  public Key publicKeyFor (Key privateKey)
  {
    return new Key (keyHash.hash (privateKey.data));
  }
  
  /**
   * Match a producer/consumer keyset in the current scheme.
   * 
   * @param producerKeys The producer keys.
   * @param consumerKeys The consumer keys.
   * @return True if a consumer using consumerKeys could receive a
   *         notification from a producer with producerKeys in this
   *         scheme.
   */
  public boolean match (KeySet producerKeys, KeySet consumerKeys)
  {
    if (isDual ())
    {
      DualKeySet keys1 = (DualKeySet)producerKeys;
      DualKeySet keys2 = (DualKeySet)consumerKeys;
      
      return match (keys1.producerKeys, keys2.producerKeys) &&
             match (keys2.consumerKeys, keys1.consumerKeys);
      
    } else if (producer)
    {
      return match ((Set<Key>)producerKeys, (Set<Key>)consumerKeys);
    } else
    {
      return match ((Set<Key>)consumerKeys, (Set<Key>)producerKeys);
    }
  }
  
  /**
   * Match a set of private keys with a set of public keys.
   * 
   * @param privateKeys A set of private (aka raw) keys.
   * @param publicKeys A set of public (aka prime) keys.
   * @return True if at least one private key mapped to its public
   *         version (using this scheme's hash) was in the given
   *         public key set.
   */
  private boolean match (Set<Key> privateKeys, Set<Key> publicKeys)
  {
    for (Key privateKey : privateKeys)
    {
      if (publicKeys.contains (publicKeyFor (privateKey)))
        return true;
    }
    
    return false;
  }

  @Override
  public boolean equals (Object object)
  {
    return object == this;
  }
  
  @Override
  public int hashCode ()
  {
    return id;
  }
  
  @Override
  public String toString ()
  {
    return name;
  }
  
  private String createName ()
  {
    StringBuilder str = new StringBuilder ();
    
    str.append (keyHash.name ()).append ('-');
    
    if (isDual ())
      str.append ("dual");
    else if (producer)
      str.append ("producer");
    else
      str.append ("consumer");
    
    return str.toString ();
  }

  /**
   * Look up the scheme for a given ID.
   *
   * @throws IllegalArgumentException if id is not a known scheme ID.
   */
  public static KeyScheme schemeFor (int id)
    throws IllegalArgumentException
  {
    switch (id)
    {
      case 1:
        return SHA1_DUAL;
      case 2:
        return SHA1_PRODUCER;
      case 3:
        return SHA1_CONSUMER;
      default:
        throw new IllegalArgumentException ("Invalid key scheme ID: " + id);    
    }
  }

  public static Set<KeyScheme> schemes ()
  {
    return SCHEMES;
  }
}
