package org.avis.federation;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

import org.avis.federation.messages.FedModify;
import org.avis.federation.messages.FedNotify;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.Message;
import org.avis.router.Router;

import static org.apache.mina.common.IoFutureListener.CLOSE;

import static org.avis.io.messages.Disconn.REASON_PROTOCOL_VIOLATION;
import static org.avis.logging.Log.warn;

public class FederationLink
{
  /**
   * Internal disconnection reason code indicating we're shutting down
   * on a Disconn request from the remote host.
   */
  private static final int REASON_SHUTDOWN_REQUESTED = -1;
  
  private IoSession session;
  private Router router;
  private IoSession connection;
  private FederationClass federationClass;
  private String serverDomain;
  private String remoteServerDomain;
  private String remoteHostName;
  
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
  }
  
  /**
   * Called when the session dies unexpectedly.
   */
  public void kill ()
  {
    // todo
  }
  
  /**
   * True if the link closed the session for any reason.
   */
  public boolean closedSession ()
  {
    return !session.containsAttribute ("linkClosed");
  }
  
  public void close ()
  {
    close (REASON_SHUTDOWN_REQUESTED, "");
  }
  
  private void close (int reason, String message)
  {
    session.setAttribute ("linkClosed");
    
    send (new Disconn (reason, message)).addListener (CLOSE);
  }
  
  public boolean isConnected ()
  {
    return connection != null && !connection.isClosing ();
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
        close ();
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
    return Federation.send (connection, serverDomain, message);
  }
}
