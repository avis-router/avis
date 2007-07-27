package org.avis.federation;

import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.federation.messages.FedConnRply;
import org.avis.federation.messages.FedConnRqst;
import org.avis.federation.messages.FedModify;
import org.avis.federation.messages.FedNotify;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.router.Router;

import static org.apache.mina.common.IoFutureListener.CLOSE;

import static org.avis.federation.EwafURI.VERSION_MAJOR;
import static org.avis.io.messages.Disconn.REASON_PROTOCOL_VIOLATION;
import static org.avis.io.messages.Disconn.REASON_SHUTDOWN;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.trace;
import static org.avis.logging.Log.warn;

public class FederationConnector
  extends FederationLink implements IoHandler
{
  /**
   * Internal disconnection reason code indicating we're shutting down
   * on a Disconn request from the remote host.
   */
  private static final int REASON_SHUTDOWN_REQUESTED = -1;

  private static final int STATE_HANDSHAKE = 0;
  private static final int STATE_LINKED = 1;
  private static final int STATE_CLOSING = 2;
  
  private EwafURI uri;
  private IoSession connection;
  private SocketConnector connector;
  private SocketConnectorConfig connectorConfig;
  private FederationClass federationClass;
  private String remoteServerDomain;
  
  protected volatile int state;
  
  public FederationConnector (Router router, String serverDomain,
                              EwafURI uri, FederationClass federationClass)
  {
    super (router, serverDomain);
    
    this.uri = uri;
    this.federationClass = federationClass;
    this.connector = new SocketConnector (1, router.executor ());
    this.state = STATE_HANDSHAKE;

    /* Change the worker timeout to make the I/O thread quit soon
     * when there's no connection to manage. */
    connector.setWorkerTimeout (0);
    
    this.connectorConfig = new SocketConnectorConfig ();
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout (20);
    
    connectorConfig.getFilterChain ().addLast
      ("codec", new ProtocolCodecFilter (FederationFrameCodec.INSTANCE));
    
    connect ();
  }
  
  void connect ()
  {
    ConnectFuture connectFuture =
      connector.connect
        (new InetSocketAddress (uri.host, uri.port),
         this, connectorConfig);

    connectFuture.addListener (new IoFutureListener ()
    {
      public void operationComplete (IoFuture f)
      {
        ConnectFuture future = (ConnectFuture)f;
        
        if (state != STATE_CLOSING && !future.isConnected ())
          connect ();
      }
    });
  }
  
  public void close ()
  {
    close (REASON_SHUTDOWN, "");
  }
  
  private void close (int reason, String message)
  {
    if (state == STATE_CLOSING)
      return;
    
    boolean wasLinked = state == STATE_LINKED;

    state = STATE_CLOSING;
    
    if (isConnected ())
    {
      if (wasLinked)
        send (new Disconn (reason, message)).addListener (CLOSE);
      else
        connection.close ();
    }
  }
  
  public boolean isConnected ()
  {
    return connection != null && !connection.isClosing ();
  }

  private void federationConnect ()
  {
    send (new FedConnRqst (VERSION_MAJOR, VERSION_MAJOR, serverDomain));
  }
  
  /**
   * Handle a message while in handshaking state.
   */
  private void handleHandshakeMessage (Message message)
  {
    switch (message.typeId ())
    {
      case FedConnRply.ID:
        handleFedConnRply ((FedConnRply)message);
        break;
      case Nack.ID:
        handleFedConnNack ((Nack)message);
        break;
      default:
        warn ("Unexpected message during handshake from remote federator at " + 
              uri + " (disconnecting): " + message.name (), this);
        close (REASON_PROTOCOL_VIOLATION, "Unexpected " + message.name ());
    }
  }
  
  /**
   * Handle a message while in linked state.
   */
  private void handleLinkMessage (Message message)
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
        close (REASON_SHUTDOWN_REQUESTED, "");
      default:
        warn ("Unexpected message from remote federator at " + uri + " (" +
              "disconnecting): " + message.name (), this);
        close (REASON_PROTOCOL_VIOLATION, "Unexpected " + message.name ());
    }
  }
  
  private void handleFedConnRply (FedConnRply reply)
  {
    state = STATE_LINKED;
    remoteServerDomain = reply.serverDomain;
    
    send (new FedModify (federationClass.incomingFilter));
  }
  
  private void handleFedConnNack (Nack nack)
  {
    warn ("Remote router at " + uri + 
          " rejected federation connect request: " + 
          nack.formattedMessage (), this);
    
    close ();
  }

  private void handleFedModify (FedModify message)
  {
    
  }

  private void handleFedNotify (FedNotify message)
  {
  }

  private WriteFuture send (Message message)
  {
    if (shouldLog (TRACE))
      trace ("Federator " + serverDomain + " sent message: " +  message, this);
    
    return connection.write (message);
  }
  
  // IoHandler  
  
  public void messageReceived (IoSession session, Object message)
    throws Exception
  {
    if (state == STATE_LINKED)
      handleLinkMessage ((Message)message);
    else if (state == STATE_HANDSHAKE)
      handleHandshakeMessage ((Message)message);
  }
  
  public void messageSent (IoSession session, Object message)
    throws Exception
  {
    // zip
  }
  
  public void sessionClosed (IoSession session)
    throws Exception
  {
    if (state == STATE_LINKED)
    {
      warn ("Router federator at " + uri + " " + 
            "closed link with no warning", this);
    } else if (state == STATE_HANDSHAKE)
    {
      warn ("Router federator at " + uri + " " + 
            "closed link during handshake, " +
            "probably due to protocol violation", this);
    }
  }
  
  public void sessionCreated (IoSession session)
    throws Exception
  {
    // zip
  }
  
  public void sessionOpened (IoSession session)
    throws Exception
  {
    connection = session;
    
    federationConnect ();
  }
  
  public void sessionIdle (IoSession session, IdleStatus status)
    throws Exception
  {
    // zip
  }
  
  public void exceptionCaught (IoSession session, Throwable cause)
    throws Exception
  {
    alarm ("Error in federator", this, cause);
  }
}
