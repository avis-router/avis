package org.avis.client;

import java.util.EventObject;

/**
 * An event sent when the connection to the Elvin router is closed.
 * 
 * @author Matthew Phillips
 */
public class CloseEvent extends EventObject
{
  /**
   * The client was shut down normally with a call to {@link Elvin#close()}.
   */
  public static final int REASON_CLIENT_SHUTDOWN = 0;
  
  /**
   * The router was shut down normally.
   */
  public static final int REASON_ROUTER_SHUTDOWN = 1;
  
  /**
   * The router failed to respond to a liveness check. Either the
   * router has crashed, or network problems have stopped messages
   * getting through.
   */
  public static final int REASON_ROUTER_STOPPED_RESPONDING = 2;
  
  /**
   * The network connection to the router was terminated abnormally
   * without the standard shutdown protocol. Most likely the network
   * connection between client and router has been disconnected.
   */
  public static final int REASON_ROUTER_SHUTDOWN_UNEXPECTEDLY = 3;
  
  /**
   * The reason for the shutdown: {@link #REASON_CLIENT_SHUTDOWN},
   * {@link #REASON_ROUTER_SHUTDOWN},
   * {@link #REASON_ROUTER_SHUTDOWN_UNEXPECTEDLY},
   * {@link #REASON_ROUTER_STOPPED_RESPONDING}.
   */
  public final int reason;

  public CloseEvent (Object source, int reason)
  {
    super (source);
    
    this.reason = reason;
  }
}
