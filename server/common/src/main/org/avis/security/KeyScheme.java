package org.avis.security;

import java.util.Set;

import static org.avis.security.SecureHash.SHA1;
import static org.avis.util.Collections.set;

/**
 * An enumeration of supported Elvin security schemes. A key scheme
 * defines a mode of sending or receiving notifications securely.
 * 
 * <h3>The Producer Scheme</h3>
 * 
 * In the producer scheme, consumers of notifications ensure that a
 * notification producer is known to them. The producer uses the
 * private key, and consumers use the public key. If the producer
 * keeps its private key secure, consumers can be assured they are
 * receiving notifications from a trusted producer.
 * 
 * <h3>The Consumer Scheme</h3>
 * 
 * In the consumer scheme, producers of notifications ensure that a
 * notification consumer is known to them, i.e. the producer controls
 * who can receive its notifications. In this scheme -- the reverse of
 * the producer scheme -- the consumer uses the private key, and
 * producers use the public key. If the consumer keeps its private key
 * secure, then the producer can be assured that only the trusted
 * consumer can receive its notifications.
 * 
 * <h3>The Dual Scheme</h3>
 * 
 * The dual scheme combines both the producer and consumer schemes, so
 * that both ends can send and receive securely. Typically both ends
 * exchange public keys, and each end then emits notifications with
 * both its private key and the public key(s) of its intended
 * consumer(s) attached. Similarly, each end would subscribe using its
 * private key and the public key(s) of its intended producer(s).
 * 
 * <h3>Avis Key Scheme API</h3>
 * 
 * The Elvin Producer and Consumer schemes both use a single set of
 * keys, whereas the Dual scheme requires both a consumer key set and
 * a producer key set. The schemes that require a single set of keys
 * are defined by an instance of {@link SingleKeyScheme}, the Dual
 * scheme is defined by an instance of {@link DualKeyScheme}.
 * <p>
 * Each key scheme also defines a {@link #keyHash secure hash} for
 * generating its public keys: see the documentation on
 * {@linkplain Key security keys} for more information on public and
 * private keys used in key schemes.
 * 
 * <h3>Supported Schemes</h3>
 * 
 * Avis currently supports just the SHA-1 secure hash as defined in
 * version 4.0 of the Elvin protocol. As such, three schemes are
 * available: {@link #SHA1_CONSUMER SHA1-Consumer},
 * {@link #SHA1_PRODUCER SHA1-Producer} and
 * {@link #SHA1_DUAL SHA1-Dual}.
 * 
 * @author Matthew Phillips
 */
public abstract class KeyScheme
{  
  /**
   * The SHA-1 Dual key scheme.
   */
  public static final DualKeyScheme SHA1_DUAL =
    new DualKeyScheme (1, SHA1);
  
  /**
   * The SHA-1 Producer key scheme.
   */
  public static final SingleKeyScheme SHA1_PRODUCER =
    new SingleKeyScheme (2, SHA1, true, false);
  
  /**
   * The SHA-1 Consumer key scheme.
   */
  public static final SingleKeyScheme SHA1_CONSUMER =
    new SingleKeyScheme (3, SHA1, false, true);

  private static final Set<KeyScheme> SCHEMES =
    set (SHA1_CONSUMER, SHA1_PRODUCER, SHA1_DUAL);
  
  /**
   * The unique ID of the scheme. This is the same as the on-the-wire
   * ID used by Elvin.
   */
  public final int id;
  
  /**
   * True if this scheme is a producer scheme.
   */
  public final boolean producer;
  
  /**
   * True of this scheme is a consumer scheme.
   */
  public final boolean consumer;
  
  /**
   * The secure hash used in this scheme.
   */
  public final SecureHash keyHash;
  
  /**
   * The unique, human-readable name of this scheme.
   */
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
  abstract boolean match (KeySet producerKeys, KeySet consumerKeys);
  
  /**
   * Match a set of private keys with a set of public keys.
   * 
   * @param sourceKeys The set of keys to check against the target
   *          set.
   * @param targetKeys If a source key is a member of this set, the
   *          keys match.
   * @return True if at least one key in the source set appears in the
   *         target set.
   */
  protected boolean matchKeys (Set<Key> sourceKeys, Set<Key> targetKeys)
  {
    for (Key sourceKey : sourceKeys)
    {
      if (targetKeys.contains (sourceKey))
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

  /**
   * The set of all supported schemes.
   */
  public static Set<KeyScheme> schemes ()
  {
    return SCHEMES;
  }
}
