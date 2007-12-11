package org.avis.client;

import java.util.EventListener;

/**
 * A listener to notifications received by any subscription created by
 * an {@linkplain Elvin elvin connection}. This differs from
 * {@linkplain NotificationListener notification listeners added to a
 * subscription} in that all notifications received by a connection
 * are available to this type of listener.
 * <p>
 * A general subscription listener can be useful in the case where
 * multiple subscriptions match a notification and the client only
 * wishes to process each notification once. Another way to handle
 * this would be to mark processed notifications with the
 * {@link AvisEventObject#setData(String, Object)} and
 * {@link AvisEventObject#getData(String)} methods.
 * 
 * @see Elvin#addNotificationListener(GeneralNotificationListener)
 * @see NotificationListener
 * 
 * @author Matthew Phillips
 */
public interface GeneralNotificationListener extends EventListener
{
  /**
   * Called when a notification is received.
   */
  public void notificationReceived (GeneralNotificationEvent e);
}
