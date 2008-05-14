package org.avis.federation;

import java.io.IOException;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.router.Router;

import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.trace;
import static org.avis.logging.Log.warn;
import static org.avis.util.Text.idFor;

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
      trace ("Federation session " + idFor (session) + " sent " + 
             message.name (), Federation.class);    }
    
    return session.write (message);
  }

  /**
   * Log a trace on message receipt.
   */
  public static void logMessageReceived (Message message, IoSession session, 
                                         Object source)
  {
    if (shouldLog (TRACE))
    {
      trace ("Federation session " + idFor (session) + " received " + 
             message.name (), source);
    }
  }
  
  /**
   * Log info about an error message from the frame codec.
   */
  public static void logError (ErrorMessage message, Object source)
  {
    warn ("Error in federation packet", source, message.error);
  }

  /**
   * Called by IO handlers to log MINA exceptions.
   */
  public static void logMinaException (Throwable cause, Object source)
  {
    if (cause instanceof IOException)
    {
      diagnostic ("I/O exception while processing federation message", 
                  source, cause);
    } else
    {
      warn ("Unexpected exception while processing federation message", 
            source, cause);
    }
  }

  public static void logSessionOpened (IoSession session, String direction,
                                       Object source)
  {
    if (shouldLog (TRACE))
    {
      trace ("Federation session " + idFor (session) + 
             " opened for " + direction + " connection on " + 
             session.getServiceAddress () + 
             (Router.isSecure (session) ? " (using TLS)" : ""), source);
    }
  }
}
