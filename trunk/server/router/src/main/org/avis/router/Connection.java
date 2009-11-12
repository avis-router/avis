package org.avis.router;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.net.InetAddress;

import org.apache.mina.core.session.IoSession;

import org.avis.config.Options;
import org.avis.security.Keys;

import static org.avis.io.Net.idFor;
import static org.avis.io.Net.remoteHostAddressFor;
import static org.avis.security.DualKeyScheme.Subset.CONSUMER;
import static org.avis.security.DualKeyScheme.Subset.PRODUCER;
/**
 * Stores the state needed for a client's connection to the router.
 * Thread access is managed via a single writer/multiple reader lock.
 * 
 * @author Matthew Phillips
 */
public class Connection
{
  /**
   * Connection options established on construction (immutable).
   */
  public ClientConnectionOptions options;

  /**
   * Connection-wide subscription keys that apply to all
   * subscriptions. Teat as immutable once assigned.
   */
  public Keys subscriptionKeys;

  /**
   * Connection-wide notification keys that apply to all
   * notifications. Treat as immutable once assigned.
   */
  public Keys notificationKeys;
  
  /**
   * The client's subscription set. Maps subscription ID's to their
   * Subscription instance.
   */
  public Long2ObjectOpenHashMap<Subscription> subscriptions;

  public int sentNotificationCount;  

  public int receivedNotificationCount;
  
  public IoSession session;

  /**
   * Single writer/multiple reader lock.
   */
  private ReentrantReadWriteLock lock;


  /**
   * Create a new connection instance.
   * 
   * @param defaultOptions The default connection options.
   * 
   * @param requestedOptions The client's requested option values.
   * @param subscriptionKeys The client's initial global subscription
   *                key collection.
   * @param notificationKeys The client's initial global notification
   *                key collection.
   */
  public Connection (IoSession session,
                     Options defaultOptions,
                     Map<String, Object> requestedOptions,
                     Keys subscriptionKeys, Keys notificationKeys)
  {
    this.session = session;
    this.subscriptions = new Long2ObjectOpenHashMap<Subscription> ();
    this.subscriptionKeys = subscriptionKeys;
    this.notificationKeys = notificationKeys;
    this.options = new ClientConnectionOptions (defaultOptions, requestedOptions);
    this.lock = new ReentrantReadWriteLock (true);
    this.sentNotificationCount = 0;
    this.receivedNotificationCount = 0;
    
    subscriptionKeys.hashPrivateKeysForRole (CONSUMER);
    notificationKeys.hashPrivateKeysForRole (PRODUCER);
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
    return keys.size () > options.getInt ("Subscription.Max-Keys");
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
      if (subscription.matches (attributes))
      {
        /*
         * Check message/global keys against global subscription keys
         * and keys for the current subscription.
         */
        boolean secureMatch = subscriptionKeys.match (globalKeys) ||
                              subscriptionKeys.match (messageKeys) ||
                              subscription.keys.match (globalKeys) ||
                              subscription.keys.match (messageKeys);

        if (secureMatch)
          matches.secure.add (subscription);
        else if (deliverInsecure && subscription.acceptInsecure)
          matches.insecure.add (subscription);
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

  public String id ()
  {
    return idFor (session);
  }
  
  public InetAddress remoteHost ()
  {
    return remoteHostAddressFor (session);
  }

  public List<Subscription> subscriptions ()
  {
    return new ArrayList<Subscription> (subscriptions.values ());
  }
}
