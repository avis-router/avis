package org.avis.federation;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

import org.avis.federation.messages.Ack;
import org.avis.federation.messages.FedModify;
import org.avis.federation.messages.FedNotify;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.Notify;
import org.avis.router.NotifyListener;
import org.avis.router.Router;
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

/**
 * A link between two federation endpoints.
 * 
 * @author Matthew Phillips
 */
public class FederationLink implements NotifyListener
{
  /**
   * Internal disconnection reason code indicating we're shutting down
   * on a Disconn request from the remote host.
   */
  private static final int REASON_DISCONN_REQUESTED = -1;

  private static final String [] EMPTY_ROUTING = new String [0];
  
  private IoSession session;
  private Router router;
  private FederationClass federationClass;
  private String serverDomain;
  private Node remotePullFilter;
  private String remoteServerDomain;
  private String remoteHostName;
  private volatile boolean closed;
  
  public FederationLink (IoSession session,
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
    
    // todo how to we subscribe to TRUE?
    if (federationClass.incomingFilter != CONST_FALSE)
      send (new FedModify (federationClass.incomingFilter));
    
    if (federationClass.outgoingFilter != CONST_FALSE)
      router.addNotifyListener (this);
  }
  
  public boolean isClosed ()
  {
    return closed;
  }
  
  /**
   * True if this link closed the session rather than the remote host.
   */
  public boolean closedSession ()
  {
    return !session.containsAttribute ("linkClosed");
  }
  
  public void close ()
  {
    close (REASON_SHUTDOWN, "");
  }
  
  private void close (int reason, String message)
  {
    closed = true;
    
    router.removeNotifyListener (this);
    
    if (session.isConnected ())
    {
      session.setAttribute ("linkClosed");
      
      if (reason == REASON_DISCONN_REQUESTED)
        session.close ();
      else
        send (new Disconn (reason, message)).addListener (CLOSE);
    }
  }
  
  /**
   * Called by router when a notification is delivered.
   */
  public void notifyReceived (Notify message)
  {
    if (closed)
      return;
    
    if (shouldPush (message))
      send (new FedNotify (message, localDomainAddedTo (routingFor (message))));
  }

  /**
   * Test if we should push a given notification to the remote router.
   */
  private boolean shouldPush (Notify message)
  {
    return routingDoesNotContain (message, remoteServerDomain) &&
           matches (federationClass.outgoingFilter, message) &&
           matches (remotePullFilter, message);
  }

  /**
   * Test if we should pull a notification sent by the remote router.
   */
  private boolean shouldPull (FedNotify message)
  {
    return routingDoesNotContain (message, serverDomain) &&
           matches (federationClass.incomingFilter, message);
  }
  
  /**
   * Return a routing list with the federator's local server domain added.
   */
  private String [] localDomainAddedTo (String [] routing)
  {
    return addDomain (routing, serverDomain);
  }
  
  public void handleMessage (Message message)
  {
    switch (message.typeId ())
    {
      case FedModify.ID:
        handleFedModify ((FedModify)message);
        break;
      case FedNotify.ID:
        handleFedNotify ((FedNotify)message);
        break;
      case Disconn.ID:
        close (REASON_DISCONN_REQUESTED, "");
        break;
      case Nack.ID:
        handleNack ((Nack)message);
        break;
      case Ack.ID:
        handleAck ((Ack)message);
        break;
      case ErrorMessage.ID:
        handleError ((ErrorMessage)message);
        break;
      default:
        warn ("Unexpected message from remote federator at " + 
              remoteHostName + " (disconnecting): " + message.name (), this);
        close (REASON_PROTOCOL_VIOLATION, "Unexpected " + message.name ());
    }
  }

  private void handleError (ErrorMessage message)
  {
    logError (message, this);
    
    close (REASON_PROTOCOL_VIOLATION, message.error.getMessage ());
  }

  /**
   * 
   * @param message
   */
  private void handleAck (Ack message)
  {
    // todo
  }

  /**
   * 
   * @param message
   */
  private void handleNack (Nack message)
  {
    // todo
  }

  private void handleFedModify (FedModify message)
  {
    remotePullFilter = message.incomingFilter;
    
    send (new Ack (message));
  }

  private void handleFedNotify (FedNotify message)
  {
    if (shouldLog (TRACE))
    {
      trace ("Federator for domain \"" + serverDomain + "\" " + 
             "FedNotify: " +
             "routing=" + asList (message.routing) + " " +
             "attributes=" + message.attributes, this);
    }
    
    if (containsDomain (message.routing, remoteServerDomain))
    {
      if (shouldPull (message))
        router.injectNotify (message);
    } else
    {
      warn ("Closing federation link on receipt of FedNotify from remote " +
            "federator that has not added its own domain (" + 
            remoteServerDomain + ") to the routing list: " + 
            asList (message.routing), this);
      
      close (REASON_PROTOCOL_VIOLATION, 
             "Remote server domain was not in FedNotify routing list");
    }
  }

  private WriteFuture send (Message message)
  {
    return Federation.send (session, serverDomain, message);
  }
  
  /**
   * Test if a given message matches an AST filter.
   */
  private static boolean matches (Node filter, Notify message)
  {
    return filter.evaluate (message.attributes) == Node.TRUE;
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
   * True if message is either not a FedNotify, or if it is but does
   * not contain the given server domain.
   */
  private static boolean routingDoesNotContain (Notify message, String domain)
  {
    return !containsDomain (routingFor (message), domain);
  }
  
  /**
   * True if the routing list contains a given server domain.
   */
  private static boolean containsDomain (String [] routing, String serverDomain)
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
