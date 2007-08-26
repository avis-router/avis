package org.avis.federation;

import java.util.Map;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

import org.avis.federation.io.messages.Ack;
import org.avis.federation.io.messages.FedNotify;
import org.avis.federation.io.messages.FedSubReplace;
import org.avis.io.messages.ConfConn;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.DropWarn;
import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.LivenessTimeoutMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.Notify;
import org.avis.io.messages.RequestMessage;
import org.avis.io.messages.RequestTimeoutMessage;
import org.avis.io.messages.TestConn;
import org.avis.router.NotifyListener;
import org.avis.router.Router;
import org.avis.security.Keys;
import org.avis.subscription.ast.Node;

import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;
import static org.apache.mina.common.IoFutureListener.CLOSE;

import static org.avis.federation.Federation.logError;
import static org.avis.io.messages.Disconn.REASON_PROTOCOL_VIOLATION;
import static org.avis.io.messages.Disconn.REASON_SHUTDOWN;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.trace;
import static org.avis.logging.Log.warn;
import static org.avis.subscription.ast.nodes.Const.CONST_FALSE;
import static org.avis.util.Collections.union;

/**
 * Manages link between two federation endpoints. This link is
 * typically established by either a {@link Acceptor} or
 * {@link Connector}.
 * 
 * @author Matthew Phillips
 */
public class Link implements NotifyListener
{
  /**
   * Internal disconnection reason code indicating we're shutting down
   * on a Disconn request from the remote host.
   */
  private static final int REASON_DISCONN_REQUESTED = -1;
  
  /**
   * Internal disconnection reason code indicating we're shutting down
   * because the remote federator vetoed a request.
   */
  private static final int REASON_REQUEST_REJECTED = -2;
  
  /**
   * Internal disconnection reason code indicating remote federator failed
   * a liveness check.
   */
  private static final int REASON_FEDERATOR_STOPPED_RESPONDING = -3;

  private static final String [] EMPTY_ROUTING = new String [0];
  
  private IoSession session;
  private Router router;
  private FederationClass federationClass;
  private String serverDomain;
  private Node remotePullFilter;
  private String remoteServerDomain;
  private String remoteHostName;
  private volatile boolean closed;

  public Link (IoSession session,
               Router router, 
               FederationClass federationClass, 
               String serverDomain, 
               String remoteServerDomain,
               String remoteHostName)
  {
    this.session = session;
    this.router = router;
    this.federationClass = federationClass;
    this.serverDomain = serverDomain;
    this.remoteServerDomain = remoteServerDomain;
    this.remoteHostName = remoteHostName;
    this.remotePullFilter = CONST_FALSE;
    
    subscribe ();
    
    if (federationClass.outgoingFilter != CONST_FALSE)
      router.addNotifyListener (this);
  }
  
  public IoSession session ()
  {
    return session;
  }
  
  public String remoteServerDomain ()
  {
    return remoteServerDomain;
  }

  public boolean isClosed ()
  {
    return closed;
  }
  
  /**
   * True if this link closed the session rather than the remote host.
   */
  public boolean initiatedSessionClose ()
  {
    return session.containsAttribute ("linkClosed");
  }
  
  public void close ()
  {
    close (REASON_SHUTDOWN, "");
  }
  
  private void close (int reason, String message)
  {
    if (closed)
      return;
    
    closed = true;
    
    router.removeNotifyListener (this);

    if (session.isConnected ())
    {
      session.setAttribute ("linkClosed");

      // just close session for internal codes
      if (reason < 0)
        session.close ();
      else
        send (new Disconn (reason, message)).addListener (CLOSE);
    }
  }
  
  private void subscribe ()
  {
    if (federationClass.incomingFilter != CONST_FALSE)
      send (new FedSubReplace (federationClass.incomingFilter));
  }
  
  /**
   * Called by router when a notification is delivered.
   */
  public void notifyReceived (Notify message, Keys keys)
  {
    String [] routing = routingFor (message);
    Map<String, Object> attributes = 
      union (message.attributes, federationClass.outgoingAttributes);
    
    if (shouldPush (routing, attributes))
    {
      send (new FedNotify (message, attributes, 
                           keys.addedTo (message.keys), 
                           serverDomainAddedTo (routing)));
    }
  }

  /**
   * Test if we should push a given notification to the remote router.
   */
  private boolean shouldPush (String [] routing, 
                              Map<String, Object> attributes)
  {
    return !containsDomain (routing, remoteServerDomain) &&
           matches (federationClass.outgoingFilter, attributes) &&
           matches (remotePullFilter, attributes);
  }

  /**
   * Test if we should pull a notification sent by the remote router.
   */
  private boolean shouldPull (FedNotify message)
  {
    return !containsDomain (message.routing, serverDomain) &&
           matches (federationClass.incomingFilter, message.attributes);
  }
  
  /**
   * Return a routing list with the federator's local server domain added.
   */
  private String [] serverDomainAddedTo (String [] routing)
  {
    return addDomain (routing, serverDomain);
  }
  
  public void handleMessage (Message message)
  {
    switch (message.typeId ())
    {
      case FedSubReplace.ID:
        handleFedSubReplace ((FedSubReplace)message);
        break;
      case FedNotify.ID:
        handleFedNotify ((FedNotify)message);
        break;
      case Disconn.ID:
        close (REASON_DISCONN_REQUESTED, "");
        break;
      case TestConn.ID:
        handleTestConn ();
        break;
      case DropWarn.ID:
        handleDropWarn ();
        break;
      case Nack.ID:
        handleNack ((Nack)message);
        break;
      case Ack.ID:
        // zip
        break;
      case RequestTimeoutMessage.ID:
        handleRequestTimeout (((RequestTimeoutMessage)message).request);
        break;
      case LivenessTimeoutMessage.ID:
        handleLivenessTimeout ();
        break;
      case ErrorMessage.ID:
        handleError ((ErrorMessage)message);
        break;
      default:
        handleProtocolViolation ("Unexpected " + message.name ());
    }
  }

  private void handleDropWarn ()
  {
    warn ("Remote federator sent a dropped packet warning: " +
          "a message may have been discarded due to congestion", this);
  }

  private void handleTestConn ()
  {
    if (session.getScheduledWriteRequests () == 0)
      send (ConfConn.INSTANCE);
  }

  private void handleRequestTimeout (RequestMessage<?> request)
  {
    if (request.getClass () == FedSubReplace.class)
    {
      warn ("Federation subscription request to remote federator at " + 
            remoteHostName + " timed out: retrying", this);
      
      subscribe ();
    } else
    {
      // NB: this shouldn't happen, FedSubReplace is the only request we send
      warn ("Request to remote federator timed out: " + request.name (), this);
    }
  }

  private void handleLivenessTimeout ()
  {
    warn ("Remote federator at " + remoteHostName + 
          " has stopped responding", this);
    
    close (REASON_FEDERATOR_STOPPED_RESPONDING, "");
  }
  
  private void handleError (ErrorMessage message)
  {
    logError (message, this);
    
    handleProtocolViolation (message.error.getMessage ());
  }

  private void handleNack (Nack nack)
  {
    warn ("Disconnecting from remote federator at " + remoteHostName + " " + 
          "after it rejected a " + nack.request.name (), this);
    
    close (REASON_REQUEST_REJECTED, "");
  }

  private void handleFedSubReplace (FedSubReplace message)
  {
    remotePullFilter = message.incomingFilter.inlineConstants ();
    
    send (new Ack (message));
  }

  private void handleFedNotify (FedNotify message)
  {
    if (shouldLog (TRACE))
    {
      trace ("Federator for domain \"" + serverDomain + "\" " + 
             "FedNotify: routing=" + asList (message.routing), this);
    }
    
    if (containsDomain (message.routing, remoteServerDomain))
    {
      message.attributes =
        union (message.attributes, federationClass.incomingAttributes);
      
      if (shouldPull (message))
        router.injectNotify (message);
    } else
    {
      warn ("Closing federation link on receipt of FedNotify from remote " +
            "federator that has not added its own domain (" + 
            remoteServerDomain + ") to the routing list: " + 
            asList (message.routing), this);
      
      handleProtocolViolation 
        ("Remote server domain was not in FedNotify routing list");
    }
  }
  
  private void handleProtocolViolation (String message)
  {
    warn ("Disconnecting remote federator at " + remoteHostName + " " + 
          "due to protocol violation: " + message, this);
    
    close (REASON_PROTOCOL_VIOLATION, message);
  }

  private WriteFuture send (Message message)
  {
    return Federation.send (session, serverDomain, message);
  }
  
  /**
   * Test if a given message matches an AST filter.
   */
  private static boolean matches (Node filter, Map<String, Object> attributes)
  {
    return filter.evaluate (attributes) == Node.TRUE;
  }
  
  /**
   * The routing list for a given notification.
   */
  private static String [] routingFor (Notify message)
  {
    if (message instanceof FedNotify)
      return ((FedNotify)message).routing;
    else
      return EMPTY_ROUTING;
  }
  
  /**
   * True if the routing list contains a given server domain.
   */
  private static boolean containsDomain (String [] routing, 
                                         String serverDomain)
  {
    for (String domain : routing)
    {
      if (domain.equals (serverDomain))
        return true;
    }
    
    return false;
  }
  
  /**
   * Add a server domain to the start of an existing routing list.
   */
  private static String [] addDomain (String [] routing, String serverDomain)
  {
    String [] newRouting = new String [routing.length + 1];
    
    newRouting [0] = serverDomain;
    
    arraycopy (routing, 0, newRouting, 1, routing.length);
    
    return newRouting;
  }
}
