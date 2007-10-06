package org.avis.security;

/**
 * An enumeration of supported secure hash algorithms.
 * 
 * @author Matthew Phillips
 */
public enum SecureHash
{
  SHA1
  {
    @Override
    public byte [] hash (byte [] input)
    {
      SHA1 sha1 = new SHA1 ();
      
      sha1.engineUpdate (input, 0, input.length);
      
      return sha1.engineDigest ();
    }
  };
  
  /**
   * Perform the hash scheme on an input byte array.
   * 
   * @param input The data to hash.
   * @return The hashed result. Length depends on the hash scheme.
   */
  public abstract byte [] hash (byte [] input);
}
