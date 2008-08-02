package org.avis.router;

import org.avis.io.messages.Notify;
import org.avis.security.Keys;

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
   * 
   * @param message The message.
   * @param keys The global notification keys that apply to the message.
   */
  public void notifyReceived (Notify message, Keys keys);
}
