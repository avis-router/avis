package org.avis.router;

import org.avis.io.messages.Notify;

/**
 * Interface for listeners to router Notify messages.
 * 
 * @author Matthew Phillips
 */
public interface NotifyListener
{
  /**
   * Invoked when the router has received a Notify message for
   * delivery.
   */
  public void notifyReceived (Notify message);
}
