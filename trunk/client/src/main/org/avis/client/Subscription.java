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
   * A subscription is inactive after a {@link #remove()} or when its
   * underlying connection is closed.
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

  /**
   * The subscription expression.
   */
  public String subscriptionExpr ()
  {
    return subscriptionExpr;
  }
  
  /**
   * Change the subscription expression.
   * 
   * @todo Use custom exception for invalid subscriptions.
   * 
   * @throws IOException if the subscription is invalid or if a
   *           network error occurs.
   */
  public void setSubscriptionExpr (String newSubscriptionExpr)
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
   * The secure mode specified for receipt of notifications.
   */
  public SecureMode secureMode ()
  {
    return secureMode;
  }
  
  /**
   * Change the subscription's secure delivery requirement.
   * 
   * @param newMode The secure delievry mode.
   * 
   * @throws IOException if an IO error occurs during the operation.
   */
  public void setSecureMode (SecureMode newMode)
    throws IOException
  {
    synchronized (elvin)
    {
      checkLive ();
      
      if (newMode != secureMode)
      {
        elvin.modifySecureMode (this, newMode);
      
        this.secureMode = newMode;
      }
    }
  }     
  
  /**
   * True if ALLOW_INSECURE_DELIVERY is enabled.
   * 
   * @see #secureMode()
   */
  public boolean acceptInsecure ()
  {
    return secureMode == ALLOW_INSECURE_DELIVERY;
  }

  /**
   * The keys used to receive secure notifications.
   */
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
   * Add a listener for notifications matched by this subscription.
   * 
   * @see Elvin#addNotificationListener(GeneralNotificationListener)
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
  
  /**
   * True if any listeners are in the listener list.
   * 
   * @see #addNotificationListener(NotificationListener)
   */
  public boolean hasListeners ()
  {
    return notificationListeners.hasListeners ();
  }

  void notifyListeners (NotificationEvent event)
  {
    notificationListeners.fire ("notificationReceived", event);
  }
}
