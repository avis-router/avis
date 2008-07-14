package org.avis.client;

import java.util.Map;

/**
 * A notification event sent to subscription listeners.
 * 
 * @see Subscription#addListener(NotificationListener)
 * 
 * @author Matthew Phillips
 */
public final class NotificationEvent extends AvisEventObject
{
  /**
   * The subscription that matched the notification. This is the same
   * as {@link #getSource()}.
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

  NotificationEvent (Subscription subscription,
                     Notification notification,
                     boolean secure,
                     Map<String, Object> data)
  {
    super (subscription, data);
    
    this.subscription = subscription;
    this.notification = notification;
    this.secure = secure;
  }
}
