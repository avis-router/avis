package org.avis.util;

/**
 * A generic pair of items.
 * 
 * @author Matthew Phillips
 */
public class Pair<T>
{
  public T item1;
  public T item2;
  
  public Pair ()
  {
    // zip
  }

  public Pair (T item1, T item2)
  {
    this.item1 = item1;
    this.item2 = item2;
  }
}
