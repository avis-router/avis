package org.avis.net.client;

import java.io.IOException;

import org.avis.net.security.Keys;
import org.avis.util.ListenerList;

public class Subscription
{
  Elvin elvin;
  String subscriptionExpr;
  boolean acceptInsecure;
  Keys keys;
  long id;
  
  private ListenerList<NotificationListener> notificationListeners;

  Subscription (String subscriptionExpr, Keys keys)
  {
    this.subscriptionExpr = subscriptionExpr;
    this.acceptInsecure = true;
    this.keys = keys;
    this.notificationListeners = new ListenerList<NotificationListener> ();
  }
  
  public void remove ()
    throws IOException
  {
    synchronized (elvin)
    {
      checkLive ();
      
      elvin.unsubscribe (this);
      subscriptionExpr = null;
    }    
  }
  
  public Elvin elvin ()
  {
    return elvin;
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

  public boolean acceptInsecure ()
  {
    return acceptInsecure;
  }

  public Keys keys ()
  {
    return keys;
  }
  
  public void setKeys (Keys newKeys)
    throws IOException
  {
    synchronized (elvin)
    {
      checkLive ();
      
      elvin.setKeys (this, newKeys);
      
      this.keys = newKeys;
    }    
  }
  
  public void setSubscription (String newSubscriptionExpr)
    throws IOException
  {
    newSubscriptionExpr = newSubscriptionExpr.trim ();
    
    if (newSubscriptionExpr.length () == 0)
      throw new IllegalArgumentException
        ("Subscription expression cannot be empty");
    
    synchronized (elvin)
    {
      checkLive ();
      
      elvin.setSubscription (this, newSubscriptionExpr);
      
      this.subscriptionExpr = newSubscriptionExpr;
    }
  }

  public void addNotificationListener (NotificationListener listener)
  {
    notificationListeners.add (listener);
  }
  
  public void removeNotificationListener (NotificationListener listener)
  {
    notificationListeners.remove (listener);
  }

  void notifyListeners (NotificationEvent event)
  {
    notificationListeners.fire ("notificationReceived", event);
  }
}
