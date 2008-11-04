package org.avis.client;

import java.util.EventListener;

/**
 * A listener to the close event sent when the client's connection to
 * the router is disconnected.
 *
 * @see Elvin#addCloseListener(CloseListener)
 */
public interface CloseListener extends EventListener
{
  /**
   * Called when the connection to the Elvin router is closed.
   */
  public void connectionClosed (CloseEvent e);
}
