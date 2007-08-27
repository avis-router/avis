package org.avis.security;

/**
 * A key scheme that requires a single key set. e.g. SHA-1 Consumer.
 * 
 * @author Matthew Phillips
 */
public final class SingleKeyScheme extends KeyScheme
{
  SingleKeyScheme (int id, SecureHash keyHash, 
                   boolean producer, boolean consumer)
  {
    super (id, keyHash, producer, consumer);
  }
}
