package org.avis.security;

/**
 * A key scheme that requires a pair of keys e.g. SHA-1 Dual.
 * 
 * @author Matthew Phillips
 */
public final class DualKeyScheme extends KeyScheme
{
  public static final int PRODUCER = 0;
  public static final int CONSUMER = 1;
  
  DualKeyScheme (int id, SecureHash keyHash)
  {
    super (id, keyHash, true, true);
  }
  
  /**
   * Check whether the parameter is a valid PRODUCER/CONSUMER value.
   */
  protected static void checkProdOrCon (int prodOrCon)
  {
    if (prodOrCon != PRODUCER && prodOrCon != CONSUMER)
      throw new IllegalArgumentException ("Not a valid producer/consumer code");
  }
}
