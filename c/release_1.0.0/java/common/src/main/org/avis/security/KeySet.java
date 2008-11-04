package org.avis.security;

/**
 * A polymorphic key set stored as part of a {@link Keys} key
 * collection: may be either a single set of Key items or a dual set
 * for the dual key schemes. Clients should not generally need to
 * access key sets directly: use the {@link Keys} class instead.
 * 
 * @author Matthew Phillips
 */
interface KeySet
{
  public int size ();
  
  public boolean isEmpty ();
  
  public void add (KeySet keys)
    throws IllegalArgumentException;
  
  public void remove (KeySet keys)
    throws IllegalArgumentException;
  
  public boolean add (Key key)
     throws IllegalArgumentException, UnsupportedOperationException;
  
  public boolean remove (Key key)
    throws IllegalArgumentException, UnsupportedOperationException;

  /**
   * Return this key with the given set removed.
   */
  public KeySet subtract (KeySet keys);
}
