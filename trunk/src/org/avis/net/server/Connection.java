package org.avis.net.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.avis.net.common.ConnectionOptions;
import org.avis.net.security.Keys;

/**
 * Stores the state needed for a client's connection to the router.
 * The operations that support the client's subscriptions are
 * thread-safe, allowing one session to modify the subscription (e.g.
 * SubModify) while another is testing for notification matches (e.g.
 * as a result of a NotifyEmit).
 * 
 * @author Matthew Phillips
 */
class Connection
{
  /**
   * Connection options established on construction (immutable).
   */
  public ConnectionOptions options;

  /**
   * Connection-wide subscription keys that apply to all
   * subscriptions. Not thread safe: treat instances as immutable once
   * assigned.
   */
  public Keys subscriptionKeys;

  /**
   * Connection-wide notification keys that apply to all
   * notifications. Not thread safe: treat instances as immutable once
   * assigned.
   */
  public Keys notificationKeys;
  
  /**
   * The client's subscription set. Maps subscription ID's to their
   * {@link Subscription} instance. Thread safe: may be safely
   * accessed and modified across sessions.
   */
  private Map<Long, Subscription> subscriptions;

  /**
   * TODO opt: could look at using the concept of a SeqLock for this
   * instead of a traditional lock. For our situation where we mainly
   * read, SeqLock's greatly reduce cache invalidation due to repeatedly
   * modifying the lock.
   * See http://blogs.sun.com/dave/entry/seqlocks_in_java.
   */
  private ReentrantReadWriteLock lock;

  public Connection (Map<String, Object> options,
                     Keys subscriptionKeys, Keys notificationKeys)
  {
    this.subscriptions = new ConcurrentHashMap<Long, Subscription> ();
    this.subscriptionKeys = subscriptionKeys;
    this.notificationKeys = notificationKeys;
    this.options = new ConnectionOptions (options);
    this.lock = new ReentrantReadWriteLock (true);
  }

  /**
   * Mark connection as closed. OK to call more than once.
   */
  public void close ()
  {
    // use null options as marker for closed connection
    options = null;
  }
  
  public boolean isOpen ()
  {
    return options != null;
  }
  
  /**
   * Lock the connection for writing. There can be only one writer and
   * zero readers at any one time.
   */
  public void lockWrite ()
  {
    lock.writeLock ().lock ();
  }

  public void unlockWrite ()
  {
    lock.writeLock ().unlock ();
  }
  
  /**
   * Lock the connection for reading. There can be any number of
   * readers and zero writers at any one time.
   */
  public void lockRead ()
  {
    lock.readLock ().lock ();
  }

  public void unlockRead ()
  {
    lock.readLock ().unlock ();
  }
  
  public void addSubscription (Subscription sub)
  {
    subscriptions.put (sub.id, sub);
  }

  public Subscription removeSubscription (long subscriptionId)
  {
    return subscriptions.remove (subscriptionId);
  }
  
  /**
   * Test if subscriptions are at or exceed the limit set by the
   * Subscription.Max-Count connection option.
   */
  public boolean subscriptionsFull ()
  {
    return subscriptions.size () >= options.getInt ("Subscription.Max-Count");
  }
  
  public boolean subscriptionTooLong (String expr)
  {
    return expr.length () > options.getInt ("Subscription.Max-Length");
  }
  
  public boolean connectionKeysFull (Keys ntfnKeys, Keys subKeys)
  {
    int maxKeys = options.getInt ("Connection.Max-Keys");
    
    return ntfnKeys.size () > maxKeys || subKeys.size () > maxKeys;
  }
  
  public boolean subscriptionKeysFull (Keys keys)
  {
    int maxKeys = options.getInt ("Subscription.Max-Keys");
    
    return keys.size () > maxKeys;
  }

  /**
   * Match a given set of attributes against this connection's
   * subscriptions and return the ID's of those subscriptions that
   * match.
   * 
   * @param attributes The attributes to match.
   * @param globalKeys The set of notification keys that apply to all
   *          notifications.
   * @param messageKeys The set of keys provided for the current
   *          notification. Either these keys or globalKeys must match
   *          a subscription's keys for a secure match to apply.
   * @param deliverInsecure If true, insecure matches are acceptable
   *          for subscriptions that allow insecure delivery.
   * 
   * @return The match result.
   */
  public SubscriptionMatch matchSubscriptions (Map<String, Object> attributes,
                                               Keys globalKeys,
                                               Keys messageKeys,
                                               boolean deliverInsecure)
  {
    SubscriptionMatch matches = new SubscriptionMatch ();
    
    for (Subscription subscription : subscriptions.values ())
    {
      /*
       * Check message/global keys against global subscription keys
       * and keys for the current subscription.
       */
      boolean secureMatch = subscriptionKeys.match (globalKeys) ||
                            subscriptionKeys.match (messageKeys) ||
                            subscription.keys.match (globalKeys) ||
                            subscription.keys.match (messageKeys);
      
      if (secureMatch || (deliverInsecure && subscription.acceptInsecure))
      {
        if (subscription.matches (attributes))
        {
          if (secureMatch)
            matches.secure.add (subscription.id);
          else
            matches.insecure.add (subscription.id);
        }
      }
    }
    
    return matches;
  }

  public Subscription subscriptionFor (long id)
    throws InvalidSubscriptionException
  {
    Subscription subscription = subscriptions.get (id);
    
    if (subscription != null)
      return subscription;
    else
      throw new InvalidSubscriptionException ("No subscription with ID " + id);
  }
}
