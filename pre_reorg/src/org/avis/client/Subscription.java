package org.avis.client;

import java.io.IOException;

import org.avis.security.Keys;
import org.avis.util.ListenerList;

import static org.avis.client.SecureMode.ALLOW_INSECURE_DELIVERY;

/**
 * A subscription to notifications from an Elvin connection.
 *
 * @see Elvin#subscribe(String, SecureMode, Keys)
 * @see #addNotificationListener(NotificationListener)
 * 
 * @author Matthew Phillips
 */
public final class Subscription
{
  Elvin elvin;
  String subscriptionExpr;
  SecureMode secureMode;
  Keys keys;
  long id;
  ListenerList<NotificationListener> notificationListeners;

  Subscription (Elvin elvin,
                String subscriptionExpr, SecureMode secureMode, Keys keys)
  {
    this.elvin = elvin;
    this.subscriptionExpr = subscriptionExpr;
    this.secureMode = secureMode;
    this.keys = keys;
    this.notificationListeners = new ListenerList<NotificationListener> ();
  }
  
  /**
   * Remove this subscription (unsubscribe). May be called more than once.
   * 
   * @throws IOException if a network error occurs.
   */
  public void remove ()
    throws IOException
  {
    synchronized (elvin)
    {
      if (id == 0)
        return;
      
      elvin.unsubscribe (this);
      
      id = 0;
    }    
  }
  
  /**
   * The elvin connection that created this subscription.
   */
  public Elvin elvin ()
  {
    return elvin;
  }
  
  /**
   * Test if this subscription is still able to receive notifications.
   */
  public boolean isActive ()
  {
    synchronized (elvin)
    {
      return id != 0;
    }
  }

  private void checkLive ()
  {
    if (id == 0)
      throw new IllegalStateException ("Subscription is not active");
  }

  public String subscriptionExpr ()
  {
    return subscriptionExpr;
  }

  public SecureMode secureMode ()
  {
    return secureMode;
  }
  
  /**
   * True if ALLOW_INSECURE_DELIVERY is enabled.
   */
  public boolean acceptInsecure ()
  {
    return secureMode == ALLOW_INSECURE_DELIVERY;
  }

  public Keys keys ()
  {
    return keys;
  }
  
  /**
   * Change the keys used for receiving secure notifications.
   */
  public void setKeys (Keys newKeys)
    throws IOException
  {
    if (newKeys == null)
      throw new IllegalArgumentException ("Keys cannot be null");
    
    synchronized (elvin)
    {
      checkLive ();
      
      elvin.modifyKeys (this, newKeys);
      
      this.keys = newKeys;
    }    
  }
  
  /**
   * Change the subscription expression.
   */
  public void setSubscription (String newSubscriptionExpr)
    throws IOException
  {
    if (newSubscriptionExpr == null)
      throw new IllegalArgumentException ("Subscription cannot be null");
    
    newSubscriptionExpr = newSubscriptionExpr.trim ();
    
    if (newSubscriptionExpr.length () == 0)
      throw new IllegalArgumentException
        ("Subscription expression cannot be empty");
    
    if (!newSubscriptionExpr.equals (subscriptionExpr))
    {
      synchronized (elvin)
      {
        checkLive ();
        
        elvin.modifySubscriptionExpr (this, newSubscriptionExpr);
        
        this.subscriptionExpr = newSubscriptionExpr;
      }
    }
  }

  /**
   * Add a listener for notifications matched by this subscription.
   */
  public void addNotificationListener (NotificationListener listener)
  {
    synchronized (elvin)
    {
      notificationListeners.add (listener);
    }
  }
  
  /**
   * Remove a previously
   * {@linkplain #addNotificationListener(NotificationListener) added}
   * listener.
   */
  public void removeNotificationListener (NotificationListener listener)
  {
    synchronized (elvin)
    {
      notificationListeners.remove (listener);
    }
  }

  void notifyListeners (NotificationEvent event)
  {
    notificationListeners.fire ("notificationReceived", event);
  }
}
