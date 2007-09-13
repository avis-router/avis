package org.avis.client;

import java.util.EventListener;

/**
 * A listener to notifications from a {@linkplain Subscription subscription}.
 *  
 * @see Subscription#addListener(NotificationListener)
 * @see GeneralNotificationListener
 * 
 * @author Matthew Phillips
 */
public interface NotificationListener extends EventListener
{
  /**
   * Called when a notification is received on a subscription.
   */
  public void notificationReceived (NotificationEvent e);
}
