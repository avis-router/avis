package org.avis.client;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

/**
 * A notification event sent to Elvin subscription listeners.
 * 
 * @see Elvin#addNotificationListener(GeneralNotificationListener)
 * 
 * @author Matthew Phillips
 */
public final class GeneralNotificationEvent extends AvisEventObject
{
  /**
   * The notification.
   */
  public final Notification notification;
  
  private final long [] insecureSubscriptionIds;
  private final long [] secureSubscriptionIds;

  private Set<Subscription> secureMatches;
  private Set<Subscription> insecureMatches;

  private Set<Subscription> matches;

  GeneralNotificationEvent (Elvin elvin,
                            Notification notification,
                            long [] insecureSubscriptionIds,
                            long [] secureSubscriptionIds)
  {
    super (elvin);
    
    this.notification = notification;
    this.insecureSubscriptionIds = insecureSubscriptionIds;
    this.secureSubscriptionIds = secureSubscriptionIds;
  }
  
  /**
   * The Elvin connection that the notification was received from.
   * This is the same as {@link #getSource()}.
   */
  public Elvin elvin ()
  {
    return (Elvin)getSource ();
  }
  
  /**
   * The subscriptions that securely matched the notification.
   * 
   * @see #matches()
   * @see #insecureMatches()
   */
  public Set<Subscription> secureMatches ()
  {
    if (secureMatches == null)
      secureMatches = subscriptionSetFor (secureSubscriptionIds);
    
    return secureMatches;
  }
  
  /**
   * The subscriptions that insecurely matched the notification.
   * 
   * @see #matches()
   * @see #secureMatches()
   */
  public Set<Subscription> insecureMatches ()
  {
    if (insecureMatches == null)
      insecureMatches = subscriptionSetFor (insecureSubscriptionIds);
    
    return insecureMatches;
  }
  
  /**
   * All subscriptions that matched the notification.
   * 
   * @see #secureMatches()
   * @see #insecureMatches()
   * 
   * @see Elvin#setKeys(org.avis.security.Keys, org.avis.security.Keys)
   * @see Subscription#setKeys(org.avis.security.Keys)
   */
  public Set<Subscription> matches ()
  {
    if (matches == null)
    {
      matches = new HashSet<Subscription> ();
      
      matches.addAll (secureMatches ());
      matches.addAll (insecureMatches ());
      
      matches = unmodifiableSet (matches);
    }
    
    return matches;
  }
  
  /**
   * Generate a subscription set for a given set of ID's
   */
  private Set<Subscription> subscriptionSetFor (long [] ids)
  {
    if (ids.length == 0)
      return emptySet ();
    
    Set<Subscription> subscriptions = new HashSet<Subscription> ();
    
    for (long id : ids)
      subscriptions.add (elvin ().subscriptionFor (id));
    
    return unmodifiableSet (subscriptions);
  }
}
