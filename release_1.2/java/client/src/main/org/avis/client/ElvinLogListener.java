package org.avis.client;

import java.util.EventListener;

/**
 * A listener for messages logged by an elvin client.
 * 
 * @see Elvin#addLogListener(ElvinLogListener)
 * @see ElvinLogEvent
 * 
 * @author Matthew Phillips
 */
public interface ElvinLogListener extends EventListener
{
  /**
   * Called when a message is logged by the elvin client.
   */
  public void messageLogged (ElvinLogEvent e);
}
