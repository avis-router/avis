package org.avis.net.security;

import static org.avis.net.common.IO.toUTF8;

import java.util.Arrays;

/**
 * A key value used to secure notifications. A key is simply an
 * immutable block of bytes. This class precomputes a hash code for
 * the key data to accelerate equals () and hashCode ().
 * 
 * @author Matthew Phillips
 */
public final class Key
{
  public final byte [] data;
  
  private int hash;
  
  public Key (String password)
  {
    this (toUTF8 (password));
  }
  
  public Key (byte [] data)
  {
    if (data == null || data.length == 0)
      throw new IllegalArgumentException ("Key data cannot be empty");
    
    this.data = data;
    this.hash = Arrays.hashCode (data);
  }

  /**
   * Shorcut to geneate the public (prime) key for a given scheme.
   * 
   * @see KeyScheme#publicKeyFor(Key)
   */
  public Key publicKeyFor (KeyScheme scheme)
  {
    return scheme.publicKeyFor (this);
  }

  @Override
  public boolean equals (Object object)
  {
    return object instanceof Key && equals ((Key)object);
  }
  
  public boolean equals (Key key)
  {
    return hash == key.hash && Arrays.equals (data, key.data);
  }
  
  @Override
  public int hashCode ()
  {
    return hash;
  }
}
