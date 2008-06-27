package org.avis.federation;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;

import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.trace;
import static org.avis.logging.Log.warn;

/**
 * General federation definitions and methods.
 * 
 * @author Matthew Phillips
 */
public final class Federation
{
  public static final int DEFAULT_EWAF_PORT = 2916;
  
  public static final int VERSION_MAJOR = 1;
  public static final int VERSION_MINOR = 0;

  private Federation ()
  {
    // zip
  }
  
  /**
   * Send a message with logging when trace enabled.
   * 
   * @param session The IO session.
   * @param serverDomain The server domain.
   * @param message The message.
   * 
   * @return The IO future.
   */
  public static WriteFuture send (IoSession session, String serverDomain,
                                  Message message)
  {
    if (shouldLog (TRACE))
    {
      trace ("Federator for domain \"" + serverDomain + "\" sent: " +  
             message, Federation.class);
    }
    
    return session.write (message);
  }

  /**
   * Log a trace on message receipt.
   */
  public static void logMessageReceived (Message message, String serverDomain, 
                                         Object source)
  {
    if (shouldLog (TRACE))
    {
      trace ("Federator for domain \"" + serverDomain + "\" " + 
             "received " + message.name (), source);
    }
  }
  
  /**
   * Log info about an error message from the frame codec.
   */
  public static void logError (ErrorMessage message, Object source)
  {
    warn ("Error in federation packet", source, message.error);
  }
}
