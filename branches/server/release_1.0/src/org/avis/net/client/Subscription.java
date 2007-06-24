package org.avis.net.client;

import org.avis.net.security.Keys;
import org.avis.util.ListenerList;

import static org.avis.net.security.Keys.EMPTY_KEYS;

public class Subscription
{
  String subscriptionExpr;
  boolean acceptInsecure;
  Keys keys;
  long id;
  
  private ListenerList<NotificationListener> notificationListeners;

  public Subscription (String subscriptionExpr)
  {
    this.subscriptionExpr = subscriptionExpr;
    this.acceptInsecure = true;
    this.keys = EMPTY_KEYS;
    this.notificationListeners = new ListenerList<NotificationListener> ();
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
