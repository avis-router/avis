package org.avis.security;

/**
 * A key scheme that requires a pair of keys. e.g. SHA-1 Dual.
 * 
 * @author Matthew Phillips
 */
public final class DualKeyScheme extends KeyScheme
{
  /**
   * Specifies which of the two subsets of a dual scheme a key is part
   * of: the producer subset (for sending notifications) or consumer
   * subset (for receiving notifications).
   */
  public enum Subset {PRODUCER, CONSUMER}
  
  DualKeyScheme (int id, SecureHash keyHash)
  {
    super (id, keyHash, true, true);
  }
  
  @Override
  boolean match (KeySet producerKeys, KeySet consumerKeys)
  {
    DualKeySet keys1 = (DualKeySet)producerKeys;
    DualKeySet keys2 = (DualKeySet)consumerKeys;
    
    return matchKeys (keys1.producerKeys, keys2.producerKeys) &&
           matchKeys (keys2.consumerKeys, keys1.consumerKeys);    
  }
}
