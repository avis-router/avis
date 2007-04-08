package org.avis.util;

/**
 * A generic change set described by two items, an added set and a
 * removed set.
 * 
 * @author Matthew Phillips
 */
public class Delta<T>
{
  public T added;
  public T removed;
  
  public Delta ()
  {
    // zip
  }

  public Delta (T added, T removed)
  {
    this.added = added;
    this.removed = removed;
  }
}
