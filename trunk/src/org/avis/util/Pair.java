package org.avis.util;

/**
 * A generic pair of items.
 * 
 * @author Matthew Phillips
 */
public class Pair<T1, T2>
{
  public T1 item1;
  public T2 item2;
  
  public Pair ()
  {
    // zip
  }

  public Pair (T1 item1, T2 item2)
  {
    this.item1 = item1;
    this.item2 = item2;
  }
}
