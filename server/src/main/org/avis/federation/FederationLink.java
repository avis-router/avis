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
import static org.apache.mina.common.IoFutureListener.CLOSE;

import static org.avis.federation.Federation.logError;
import static org.avis.io.messages.Disconn.REASON_PROTOCOL_VIOLATION;
import static org.avis.io.messages.Disconn.REASON_SHUTDOWN;
import static org.avis.logging.Log.warn;
import static org.avis.subscription.ast.nodes.Const.CONST_FALSE;

public class FederationLink implements NotifyListener
{
  /**
   * Internal disconnection reason code indicating we're shutting down
   * on a Disconn request from the remote host.
   */
  private static final int REASON_DISCONN_REQUESTED = -1;
  
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
  
  public void notifyReceived (Notify message)
  {
    if (closed)
      return;
    
    if (shouldPush (message))
      send (new FedNotify (message, routingFor (message)));
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
  private boolean shouldPull (Notify message)
  {
    return routingDoesNotContain (message, serverDomain) &&
           matches (federationClass.incomingFilter, message);
  }
  
  /**
   * Generate the routing list for a given message, taking into
   * account existing routing if it's a FedNotify.
   * 
   * @param message The source message.
   * 
   * @return The routing, including our server domain.
   */
  private String [] routingFor (Notify message)
  {
    String [] routing;
    
    if (message instanceof FedNotify)
      routing = addDomain (((FedNotify)message).routing, serverDomain);
    else
      routing = new String [] {serverDomain};
    
    return routing;
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
        logError ((ErrorMessage)message, this);
        break;
      default:
        warn ("Unexpected message from remote federator at " + 
              remoteHostName + " (disconnecting): " + message.name (), this);
        close (REASON_PROTOCOL_VIOLATION, "Unexpected " + message.name ());
    }
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
    if (shouldPull (message))
      router.injectNotify (message);
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
   * True if message is either not a FedNotify, or if it is but does
   * not contain the given server domain.
   */
  private static boolean routingDoesNotContain (Notify message,
                                                String serverDomain)
  {
    if (message instanceof FedNotify)
      return !((FedNotify)message).routingContains (serverDomain);
    else
      return true;
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
