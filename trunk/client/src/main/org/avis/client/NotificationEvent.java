package org.avis.client;

import java.util.EventObject;

/**
 * A notification delivery event.
 * 
 * @see Subscription#addNotificationListener(NotificationListener)
 * 
 * @author Matthew Phillips
 */
public final class NotificationEvent extends EventObject
{
  /**
   * The subscription that matched the notification.
   */
  public final Subscription subscription;
  
  /**
   * The notification received from the router.
   */
  public final Notification notification;
  
  /**
   * True if the notification was received securely from a client with
   * compatible security keys.
   */
  public final boolean secure;

  NotificationEvent (Elvin source,
                     Subscription subscription,
                     Notification notification,
                     boolean secure)
  {
    super (source);
    
    this.subscription = subscription;
    this.notification = notification;
    this.secure = secure;
  }
}
