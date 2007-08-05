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
import org.avis.io.messages.RequestMessage;
import org.avis.router.Router;

import static java.lang.Thread.sleep;

import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;
import static org.avis.federation.Federation.logError;
import static org.avis.federation.Federation.logMessageReceived;
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
  private RequestTracker requestTracker;
  private FederationClass federationClass;
  private String serverDomain;
  private FederationLink link;
  private InetSocketAddress remoteAddress;
  private IoSession session;
  private volatile boolean closing;
  
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
    this.closing = false;
    
    connector.connect 
      (remoteAddress, this, connectorConfig).addListener (new IoFutureListener ()
    {
      public void operationComplete (IoFuture future)
      {
        connectFutureComplete (future);
      }
    });
  }
  
  protected void connectFutureComplete (IoFuture future)
  {
    if (closing)
      return;
    
    try
    {
      if (!future.isReady ())
      {
        warn ("Connection attempt to federator at " + uri + " timed out: " +
              "retrying", this);
        
        connect ();
      } else
      {
        open (future.getSession ());
      }
    } catch (RuntimeIOException ex)
    {
      // todo handle: we get a ConnectException embedded here when refused
      alarm ("Failed to connect to federator at " + uri + ": retrying", 
             this, ex.getCause ());
      
      try
      {
        sleep (connectorConfig.getConnectTimeoutMillis ());

        connect ();
      } catch (InterruptedException ex2)
      {
        ex2.printStackTrace ();
      }
    }
  }

  void open (IoSession newSession)
  {
    this.session = newSession;
    this.requestTracker = new RequestTracker (session);
    
    send (new FedConnRqst (VERSION_MAJOR, VERSION_MINOR, serverDomain));
  }
  
  public void close ()
  {
    if (closing || session == null)
      return;
    
    closing = true;

    requestTracker.shutdown ();
    
    if (session.isConnected ())
    {
      if (link != null)
        link.close ();
      else
        session.close ();
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
    session = null;
  }
  
  private void reopen ()
  {
    close ();
    connect ();
  }

  private void handleMessage (Message message)
  {
    // todo check XID
    switch (message.typeId ())
    {
      case FedConnRply.ID:
        createFederationLink (((FedConnRply)message).serverDomain);
        break;
      case Nack.ID:
        handleFedConnNack ((Nack)message);
        break;
      case RequestTracker.TimeoutMessage.ID:
        handleRequestTimeout (((RequestTracker.TimeoutMessage)message).request);
        break;
      case ErrorMessage.ID:
        logError ((ErrorMessage)message, this);
        close ();
        break;
      default:
        warn ("Unexpected message during handshake from remote federator at " +  
              uri + ": " + message.name (), this);
        // todo add max number of retries
        reopen ();
    }
  }
  
  private void handleRequestTimeout (RequestMessage<?> request)
  {
    // sanity check
    if (!(request instanceof FedConnRqst))
      throw new Error ("Request timeout for illegal message " + request);
    
    warn ("Federation connection request to remote federator at " + 
          uri + " timed out: reconnecting", this);
    
    reopen ();
  }

  private void createFederationLink (String remoteServerDomain)
  {
    String remoteHost = remoteAddress.getHostName ();

    diagnostic ("Federation outgoing link established with " + 
                remoteHost + ", remote server domain \"" + 
                remoteServerDomain + "\"", this);
    
    link =
      new FederationLink (session, router, requestTracker,
                          federationClass, serverDomain, 
                          remoteServerDomain, remoteHost);
  }
  
  private void handleFedConnNack (Nack nack)
  {
    warn ("Closing connection to remote router at " + uri + 
          " after it rejected federation connect request: " + 
          nack.formattedMessage (), this);
    
    close ();
  }
  
  private void send (Message message)
  {
    if (message instanceof RequestMessage)
      requestTracker.add ((RequestMessage<?>)message);
    
    Federation.send (session, serverDomain, message);
  }
  
  // IoHandler  
  
  public void sessionOpened (IoSession theSession)
    throws Exception
  {
    // zip
  }
  
  public void sessionClosed (IoSession theSession)
    throws Exception
  {
    if (!closing)
    {
      warn ("Remote federator at " + uri + 
            " closed connection unexpectedly, reconnecting", this);
      
      close ();
      connect ();
    }
  }
  
  public void sessionCreated (IoSession theSession)
    throws Exception
  {
    // zip
  }
  
  public void messageReceived (IoSession theSession, Object theMessage)
    throws Exception
  {
    if (closing)
      return;
   
    Message message = (Message)theMessage;
    
    logMessageReceived (message, serverDomain, this);
    
    if (link == null)
      handleMessage (message);
    else if (!link.isClosed ())
      link.handleMessage (message);
  }
  
  public void messageSent (IoSession theSession, Object message)
    throws Exception
  {
    // zip
  }

  public void sessionIdle (IoSession theSession, IdleStatus status)
    throws Exception
  {
    // zip
  }
  
  public void exceptionCaught (IoSession theSession, Throwable cause)
    throws Exception
  {
    alarm ("Error in federator", this, cause);
  }
}
