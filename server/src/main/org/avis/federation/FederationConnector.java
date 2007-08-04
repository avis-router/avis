package org.avis.federation;

import java.io.Closeable;

import java.net.InetSocketAddress;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.federation.messages.FedConnRply;
import org.avis.federation.messages.FedConnRqst;
import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.router.Router;

import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;
import static org.avis.federation.Federation.logError;
import static org.avis.federation.Federation.logMessageReceived;
import static org.avis.federation.Federation.send;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.warn;

/**
 * The federation connector is responsible for connecting to a remote
 * host, handshaking with a FedConnRqst, after which it creates and
 * hands over processing to a FederationLink.
 * 
 * @author Matthew Phillips
 */
public class FederationConnector implements IoHandler, Closeable
{
  private EwafURI uri;
  private Router router;
  private SocketConnector connector;
  private SocketConnectorConfig connectorConfig;
  private FederationClass federationClass;
  private String serverDomain;
  private FederationLink link;
  private InetSocketAddress remoteAddress;
  private IoSession linkConnection;
  
  protected volatile boolean closing;
  
  public FederationConnector (Router router, String serverDomain,
                              EwafURI uri, FederationClass federationClass)
  {
    this.router = router;
    this.uri = uri;
    this.serverDomain = serverDomain;
    this.federationClass = federationClass;
    this.connector = new SocketConnector (1, router.executor ());
    this.connectorConfig = new SocketConnectorConfig ();
    this.remoteAddress = new InetSocketAddress (uri.host, uri.port);

    /* Change the worker timeout to make the I/O thread quit soon
     * when there's no connection to manage. */
    connector.setWorkerTimeout (0);
    
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout (20);
    
    connectorConfig.getFilterChain ().addLast
      ("codec", new ProtocolCodecFilter (FederationFrameCodec.INSTANCE));
    
    connect ();
  }
  
  void connect ()
  {
    connector.connect 
      (remoteAddress, this, connectorConfig).addListener (new IoFutureListener ()
    {
      public void operationComplete (IoFuture future)
      {
        if (closing)
          return;
        
        try
        {
          if (!future.isReady ())
            connect ();
          else
            open (future.getSession ());
        } catch (RuntimeIOException ex)
        {
          // todo handle: we get a ConnectException embedded here when refused
          alarm ("Failed to connect", this, ex);
        }
      }
    });
  }
  
  void open (IoSession session)
  {
    this.linkConnection = session;
    
    send (session, serverDomain,
          new FedConnRqst (VERSION_MAJOR, VERSION_MINOR, serverDomain));
  }
  
  public void close ()
  {
    if (closing || linkConnection == null)
      return;
    
    closing = true;
    
    if (linkConnection.isConnected ())
    {
      if (link != null)
        link.close ();
      else
        linkConnection.close ();
    } else
    {
      if (link != null && !link.closedSession ())
      {
        warn ("Remote federator at " + uri + " " + 
              "closed link with no warning", this);
        
        link.close ();
      }
    }
    
    link = null;
    linkConnection = null;
  }
  
  private void handleMessage (IoSession session, Message message)
  {
    // todo check XID
    switch (message.typeId ())
    {
      case FedConnRply.ID:
        createFederationLink (session, ((FedConnRply)message).serverDomain);
        break;
      case Nack.ID:
        handleFedConnNack (session, (Nack)message);
        break;
      case ErrorMessage.ID:
        logError ((ErrorMessage)message, this);
        session.close ();
        break;
      default:
        warn ("Unexpected message during handshake from remote federator at " + 
              uri + " (disconnecting): " + message.name (), this);
        session.close ();
    }
  }
  
  private void createFederationLink (IoSession session, 
                                     String remoteServerDomain)
  {
    String remoteHost = remoteAddress.getHostName ();

    diagnostic ("Federation outgoing link established with " + 
                remoteHost + ", remote server domain \"" + 
                remoteServerDomain + "\"", this);
    
    link =
      new FederationLink (session, router, federationClass, serverDomain, 
                          remoteServerDomain, remoteHost);
  }
  
  private void handleFedConnNack (IoSession session, Nack nack)
  {
    warn ("Remote router at " + uri + " rejected federation connect request: " + 
          nack.formattedMessage (), this);
    
    session.close ();
  }
  
  // IoHandler  
  
  public void sessionOpened (IoSession session)
    throws Exception
  {
    // zip
  }
  
  public void sessionClosed (IoSession session)
    throws Exception
  {
    close ();
  }
  
  public void sessionCreated (IoSession session)
    throws Exception
  {
    // zip
  }
  
  public void messageReceived (IoSession session, Object message)
    throws Exception
  {
    if (closing)
      return;
    
    logMessageReceived (message, serverDomain, this);
    
    if (link == null)
      handleMessage (session, (Message)message);
    else if (!link.isClosed ())
      link.handleMessage ((Message)message);
  }
  
  public void messageSent (IoSession session, Object message)
    throws Exception
  {
    // zip
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
