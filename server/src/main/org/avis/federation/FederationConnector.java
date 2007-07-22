package org.avis.federation;

import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.federation.messages.FedConnRply;
import org.avis.federation.messages.FedConnRqst;
import org.avis.federation.messages.FedModify;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.logging.Log;
import org.avis.router.Router;

import static org.avis.federation.EwafURI.VERSION_MAJOR;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.trace;
import static org.avis.logging.Log.warn;

public class FederationConnector
  extends FederationLink implements IoHandler
{
  EwafURI uri;
  IoSession connection;
  SocketConnector connector;
  SocketConnectorConfig connectorConfig;
  FederationClass federationClass;
  String remoteServerDomain;
  boolean federationLinkConnected;
  volatile boolean closing;
  
  public FederationConnector (Router router, String federationId,
                              EwafURI uri, FederationClass federationClass)
  {
    super (router, federationId);
    
    this.uri = uri;
    this.federationClass = federationClass;
    this.connector = new SocketConnector (1, router.executor ());

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
        
        if (!future.isConnected ())
          connect ();
      }
    });
  }
  
  public void close ()
  {
    closing = true;
    
    if (isConnected ())
      connection.close ().join (20000);
  }
  
  public boolean isConnected ()
  {
    return connection != null && !connection.isClosing ();
  }

  private void federationConnect ()
  {
    send (new FedConnRqst (VERSION_MAJOR, VERSION_MAJOR, federationId));
  }
  
  private void handleHandshakeMessage (Message message)
  {
    switch (message.typeId ())
    {
      case FedConnRply.ID:
        handleFedConnRply ((FedConnRply)message);
      case Nack.ID:
        handleFedConnNack ((Nack)message);
    }
  }
  
  private void handleLinkMessage (Message message)
  {
    switch (message.typeId ())
    {
      // todo
    }
  }
  
  private void handleFedConnRply (FedConnRply reply)
  {
    remoteServerDomain = reply.serverDomain;
    
    startFederationLink ();
  }
  
  private void startFederationLink ()
  {
    federationLinkConnected = true;
    
    send (new FedModify (federationClass.incomingFilter));
  }

  private void handleFedConnNack (Nack nack)
  {
    Log.warn ("Remote router at " + uri + 
              " rejected federation connect request: " + 
              nack.formattedMessage (), this);
    
    close ();
  }

  private void send (Message message)
  {
    if (shouldLog (TRACE))
      trace ("Federator " + federationId + " sent message: " +  message, this);
    
    connection.write (message);
  }
  
  // IoHandler  
  
  public void messageReceived (IoSession session, Object message)
    throws Exception
  {
    if (federationLinkConnected)
      handleLinkMessage ((Message)message);
    else
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
    if (!closing)
    {
      if (federationLinkConnected)
      {
        warn ("Router federator at " + uri + " " + 
              "closed link with no warning", this);
      } else
      {
        warn ("Router federator at " + uri + " " + 
              "closed link during handshake, " +
              "probably due to protocol violation", this);
      }
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
