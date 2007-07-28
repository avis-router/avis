package org.avis.federation;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

import org.avis.federation.messages.Ack;
import org.avis.federation.messages.FedModify;
import org.avis.federation.messages.FedNotify;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.router.Router;

import static org.apache.mina.common.IoFutureListener.CLOSE;

import static org.avis.io.messages.Disconn.REASON_PROTOCOL_VIOLATION;
import static org.avis.io.messages.Disconn.REASON_SHUTDOWN;
import static org.avis.logging.Log.warn;
import static org.avis.subscription.ast.nodes.Const.CONST_FALSE;

public class FederationLink
{
  /**
   * Internal disconnection reason code indicating we're shutting down
   * on a Disconn request from the remote host.
   */
  private static final int REASON_DISCONN_REQUESTED = -1;
  
  private IoSession session;
  @SuppressWarnings("unused")
  private Router router;
  @SuppressWarnings("unused")
  private FederationClass federationClass;
  private String serverDomain;
  @SuppressWarnings("unused")
  private String remoteServerDomain;
  private String remoteHostName;
  private boolean closed;
  
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
    
    // todo how to we subscribe to TRUE?
    if (federationClass.incomingFilter != CONST_FALSE)
      send (new FedModify (federationClass.incomingFilter));
  }
  
  public boolean isClosed ()
  {
    return closed;
  }
  
  /**
   * Called when the session dies unexpectedly.
   */
  public void kill ()
  {
    // todo
    closed = true;
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
    session.setAttribute ("linkClosed");
    
    if (reason == REASON_DISCONN_REQUESTED)
      session.close ();
    else
      send (new Disconn (reason, message)).addListener (CLOSE);
  }
  
  /**
   * Handle a message while in linked state.
   */
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

  /**
   * 
   * @param message
   */
  private void handleFedModify (FedModify message)
  {
    // todo
  }

  /**
   * 
   * @param message
   */
  private void handleFedNotify (FedNotify message)
  {
    // todo
  }

  private WriteFuture send (Message message)
  {
    return Federation.send (session, serverDomain, message);
  }
}
