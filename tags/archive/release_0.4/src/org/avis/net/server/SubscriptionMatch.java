package org.avis.net.server;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents the result of matching a subscription against a message.
 * 
 * @author Matthew Phillips
 */
class SubscriptionMatch
{
  private static final long [] EMPTY = new long [0];
  
  /** Securely matched subscription ID's */
  public Set<Long> secure;
  /** Insecurely matched subscription ID's */
  public Set<Long> insecure;
  
  public SubscriptionMatch ()
  {
    this.secure = new HashSet<Long> ();
    this.insecure = new HashSet<Long> ();
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

  private static long [] toArray (Set<Long> set)
  {
    if (set.isEmpty ())
    {
      return EMPTY;
    } else
    {
      long [] longs = new long [set.size ()];
      
      int index = 0;

      for (Long l : set)
        longs [index++] = l;
      
      return longs;
    }
  }
}
