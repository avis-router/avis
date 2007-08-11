package org.avis.federation;

import java.util.Timer;
import java.util.TimerTask;

import java.io.Closeable;

import java.net.InetSocketAddress;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.federation.messages.FedConnRply;
import org.avis.federation.messages.FedConnRqst;
import org.avis.io.RequestTrackingFilter;
import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.RequestMessage;
import org.avis.io.messages.RequestTimeoutMessage;
import org.avis.router.Router;

import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;
import static org.avis.federation.Federation.logError;
import static org.avis.federation.Federation.logMessageReceived;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.warn;
import static org.avis.util.Text.shortException;

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
  private FederationOptions options;
  private Router router;
  private SocketConnector connector;
  private SocketConnectorConfig connectorConfig;
  private FederationClass federationClass;
  private String serverDomain;
  private FederationLink link;
  private InetSocketAddress remoteAddress;
  private IoSession session;
  private volatile boolean closing;
  private Timer asyncConnectTimer;
  
  public FederationConnector (Router router, String serverDomain,
                              EwafURI uri, FederationClass federationClass,
                              FederationOptions options)
  {
    this.router = router;
    this.uri = uri;
    this.serverDomain = serverDomain;
    this.federationClass = federationClass;
    this.options = options;
    this.connector = new SocketConnector (1, router.executor ());
    this.connectorConfig = new SocketConnectorConfig ();
    this.remoteAddress = new InetSocketAddress (uri.host, uri.port);
    
    int connectTimeout = options.getInt ("Federation.Connection-Timeout");

    /* Change the worker timeout to make the I/O thread quit soon
     * when there's no connection to manage. */
    connector.setWorkerTimeout (0);
    
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout (connectTimeout);
    
    DefaultIoFilterChainBuilder filterChain = connectorConfig.getFilterChain ();
    
    filterChain.addLast ("codec", FederationFrameCodec.FILTER);
    
    filterChain.addLast ("requestTracker", 
                         new RequestTrackingFilter (connectTimeout));

    connect ();
  }
  
  /**
   * Kick off a connection attempt.
   */
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
  
  /**
   * Called by future created by connect () when complete.
   */
  protected void connectFutureComplete (IoFuture future)
  {
    if (closing)
      return;
    
    try
    {
      if (!future.isReady ())
      {
        diagnostic ("Connection attempt to federator at " + uri + 
                    " timed out, retrying", this);
        
        asyncConnect ();
      } else
      {
        open (future.getSession ());
      }
    } catch (RuntimeIOException ex)
    {
      diagnostic ("Failed to connect to federator at " + uri + ", retrying: " + 
                  shortException (ex.getCause ()), this);
      
      asyncConnect ();
    }
  }

  /**
   * Schedule a delayed connect () in Federation.Connection-Timeout
   * seconds from now.
   * 
   * @see #cancelAsyncConnect()
   */
  private void asyncConnect ()
  {
    if (asyncConnectTimer == null)
      asyncConnectTimer = new Timer ("Federation connector");
    
    TimerTask connectTask = new TimerTask ()
    {
      @Override
      public void run ()
      {
        connect ();
      }
    };
    
    asyncConnectTimer.schedule 
      (connectTask, options.getInt ("Federation.Connection-Timeout") * 1000);
  }
  
  /**
   * Canncel an async connect and its associated timer.
   * 
   * @see #asyncConnect()
   */
  private void cancelAsyncConnect ()
  {
    if (asyncConnectTimer != null)
    {
      asyncConnectTimer.cancel ();
      
      asyncConnectTimer = null;
    }
  }

  /**
   * Called to open a federation connection when a connection and
   * session has been established.
   */
  void open (IoSession newSession)
  {
    this.session = newSession;
    
    cancelAsyncConnect ();
    
    send (new FedConnRqst (VERSION_MAJOR, VERSION_MINOR, serverDomain));
  }
  
  public void close ()
  {
    if (closing || session == null)
      return;
    
    closing = true;
    
    cancelAsyncConnect ();
    
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
  
  public boolean isWaitingForAsyncConnection ()
  {
    return asyncConnectTimer != null;
  }
  
  public boolean isConnected ()
  {
    return link != null;
  }

  private void handleMessage (Message message)
  {
    switch (message.typeId ())
    {
      case FedConnRply.ID:
        createFederationLink (((FedConnRply)message).serverDomain);
        break;
      case Nack.ID:
        handleFedConnNack ((Nack)message);
        break;
      case RequestTimeoutMessage.ID:
        handleRequestTimeout (((RequestTimeoutMessage)message).request);
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
    if (request instanceof FedConnRqst)
    {
      warn ("Federation connection request to remote federator at " + 
            uri + " timed out: reconnecting", this);
      
      reopen ();
    } else
    {
     // NB: this shouldn't happen, FedConnRqst is the only request we send
      warn ("Request to remote federator timed out: " + request.name (), this);
    }
  }

  private void createFederationLink (String remoteServerDomain)
  {
    String remoteHost = remoteAddress.getHostName ();

    diagnostic ("Federation outgoing link established with " + 
                remoteHost + ", remote server domain \"" + 
                remoteServerDomain + "\"", this);
    
    link =
      new FederationLink (session, router,
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
    warn ("Unexpected exception while processing federation message", 
          this, cause);
  }
}
