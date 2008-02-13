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
   * The client was shut down normally with a call to
   * {@link Elvin#close()}.
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
   * Either the client or the router decided that the protocol rules
   * have been violated. This would only happen in the case of a
   * serious bug in the client or router.
   */
  public static final int REASON_PROTOCOL_VIOLATION = 4;
  
  /**
   * An I/O exception was thrown while communicating with the router.
   * The exception will be in the error field.
   */
  public static final int REASON_IO_ERROR = 5;
  
  /**
   * The reason for the shutdown: {@link #REASON_CLIENT_SHUTDOWN},
   * {@link #REASON_ROUTER_SHUTDOWN},
   * {@link #REASON_ROUTER_SHUTDOWN_UNEXPECTEDLY},
   * {@link #REASON_ROUTER_STOPPED_RESPONDING},
   * {@link #REASON_PROTOCOL_VIOLATION}, {@link #REASON_IO_ERROR}.
   */
  public final int reason;

  /**
   * The message from the router, if any, or the client's description
   * of the reason otherwise.
   */
  public final String message;

  /**
   * The exception that was associated with the reason for closing.
   * This may be null. It will be set in the case where an exception
   * occurred during decoding a message from the router and triggered
   * a close due to protocol violation.
   */
  public final Throwable error;

  public CloseEvent (Object source, int reason, 
                     String message, Throwable error)
  {
    super (source);
    
    this.reason = reason;
    this.message = message;
    this.error = error;
  }
}
