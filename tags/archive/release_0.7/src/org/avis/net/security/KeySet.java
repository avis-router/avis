package org.avis.net.security;

/**
 * A key set stored as part of a {@link Keys} key collection. Clients
 * should not modify key sets directly: use the {@link Keys} methods
 * instead.
 * 
 * @author Matthew Phillips
 */
public interface KeySet
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
}
