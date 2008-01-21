package org.avis.router;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

/**
 * Represents the result of matching a subscription against a message.
 * 
 * @author Matthew Phillips
 */
class SubscriptionMatch
{
  private static final long [] EMPTY = new long [0];
  
  /** Securely matched subscription ID's */
  public final LongArrayList secure;
  /** Insecurely matched subscription ID's */
  public final LongArrayList insecure;
  
  public SubscriptionMatch ()
  {
    this.secure = new LongArrayList ();
    this.insecure = new LongArrayList ();
  }
  
  public long [] secure ()
  {
    return toArray (secure);
  }
  
  public long [] insecure ()
  {
    return toArray (insecure);
  }
  
  public boolean matched ()
  {
    return !insecure.isEmpty () || !secure.isEmpty ();
  }

  private static long [] toArray (LongList ids)
  {
    if (ids.isEmpty ())
      return EMPTY;
    else
      return ids.toLongArray ();
  }
}
