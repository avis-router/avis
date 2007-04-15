package org.avis.net.client;

import java.util.EventObject;


/**
 * A notification delivery event.
 * 
 * @see Subscription#addNotificationListener(NotificationListener)
 * 
 * @author Matthew Phillips
 */
public class NotificationEvent extends EventObject
{
  public final Subscription subscription;
  public final Notification notification;
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
