package org.avis.router;

/**
 * Interface for listeners to router shutdown.
 * 
 * @author Matthew Phillips
 */
public interface CloseListener
{
  /**
   * Invoked when the router is about to shut down.
   * 
   * @param router The router.
   * 
   * @see Router#close()
   */
  public void routerClosing (Router router);
}
