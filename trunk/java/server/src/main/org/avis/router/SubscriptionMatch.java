package org.avis.router;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of matching a subscription against a message.
 * 
 * @author Matthew Phillips
 */
class SubscriptionMatch
{
  private static final long [] EMPTY = new long [0];
  
  /** Securely matched subscriptions */
  public final List<Subscription> secure;
  /** Insecurely matched subscriptions */
  public final List<Subscription> insecure;
  
  public SubscriptionMatch ()
  {
    this.secure = new ArrayList<Subscription> ();
    this.insecure = new ArrayList<Subscription> ();
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

  private static long [] toArray (List<Subscription> subscriptions)
  {
    if (subscriptions.isEmpty ())
    {
      return EMPTY;
    } else
    {
      long [] ids = new long [subscriptions.size ()];
      
      for (int i = 0; i < ids.length; i++)
        ids [i] = subscriptions.get (i).id;
      
      return ids;
    }
  }
}
