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

import org.avis.config.Options;
import org.avis.federation.io.FederationFrameCodec;
import org.avis.federation.io.messages.FedConnRply;
import org.avis.federation.io.messages.FedConnRqst;
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
 * Initiates a federation connection to a remote host. After
 * connection and successfully handshaking with a FedConnRqst, it
 * creates and hands over processing to a Link.
 * 
 * @see Link
 * @see Acceptor
 * 
 * @author Matthew Phillips
 */
public class Connector implements IoHandler, Closeable
{
  private EwafURI uri;
  private Options options;
  private Router router;
  private SocketConnector connector;
  private SocketConnectorConfig connectorConfig;
  private FederationClass federationClass;
  private String serverDomain;
  private Link link;
  private InetSocketAddress remoteAddress;
  private IoSession session;
  private Timer asyncConnectTimer;
  protected volatile boolean closing;
  
  public Connector (Router router, String serverDomain,
                    EwafURI uri, FederationClass federationClass,
                    Options options)
  {
    this.router = router;
    this.uri = uri;
    this.serverDomain = serverDomain;
    this.federationClass = federationClass;
    this.options = options;
    this.connector = new SocketConnector (1, router.executor ());
    this.connectorConfig = new SocketConnectorConfig ();
    this.remoteAddress = new InetSocketAddress (uri.host, uri.port);
    
    int requestTimeout = options.getInt ("Federation.Request-Timeout");
    int keepaliveInterval = options.getInt ("Federation.Keepalive-Interval");

    /* Change the worker timeout to make the I/O thread quit soon
     * when there's no connection to manage. */
    connector.setWorkerTimeout (0);
    
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout (requestTimeout);
    
    DefaultIoFilterChainBuilder filterChain = connectorConfig.getFilterChain ();
    
    filterChain.addLast ("codec", FederationFrameCodec.FILTER);
    
    filterChain.addLast
      ("requestTracker", 
       new RequestTrackingFilter (requestTimeout, keepaliveInterval));

    connect ();
  }
  
  public Link link ()
  {
    return link;
  }
  
  /**
   * Kick off a connection attempt.
   */
  synchronized void connect ()
  {
    closing = false;
    
    cancelAsyncConnect ();
    
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
   * Schedule a delayed connect () in Federation.Request-Timeout
   * seconds from now.
   * 
   * @see #cancelAsyncConnect()
   */
  synchronized void asyncConnect ()
  {
    if (asyncConnectTimer != null)
      throw new Error ();
    
    asyncConnectTimer = new Timer ("Federation connector");
      
    TimerTask connectTask = new TimerTask ()
    {
      @Override
      public void run ()
      {
        if (!closing)
          connect ();
      }
    };
    
    asyncConnectTimer.schedule 
      (connectTask, options.getInt ("Federation.Request-Timeout") * 1000);
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
  synchronized void open (IoSession newSession)
  {
    this.session = newSession;
    
    cancelAsyncConnect ();
    
    send (new FedConnRqst (VERSION_MAJOR, VERSION_MINOR, serverDomain));
  }
  
  public void close ()
  {
    synchronized (this)
    {
      closing = true;
      
      cancelAsyncConnect ();
      
      if (session != null)
      {
        if (session.isConnected ())
        {
          if (link != null)
            link.close ();
          else
            session.close ();
        } else
        {
          if (link != null && !link.initiatedSessionClose ())
          {
            warn ("Remote federator at " + uri + " " + 
                  "closed link with no warning", this);
            
            link.close ();
          }        
        }
        
        // wait for session to be flushed
        // todo check that this really flushes messages
        session.close ().join (10000);
        session = null;
      }
      
      link = null;
    }
  }
  
  private void reopen ()
  {
    if (closing)
      return;
    
    close ();

    diagnostic ("Scheduling reconnection for outgoing federation link to " + 
                uri, this);

    closing = false;
    
    asyncConnect ();
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
    if (request.getClass () == FedConnRqst.class)
    {
      warn ("Federation connection request to remote federator at " + 
            uri + " timed out, reconnecting", this);
      
      reopen ();
    } else
    {
     // NB: this shouldn't happen, FedConnRqst is the only request we send
      warn ("Request to remote federator timed out: " + request.name (), this);
    }
  }
  
  private void handleFedConnNack (Nack nack)
  {
    warn ("Closing connection to remote router at " + uri + 
          " after it rejected federation connect request: " + 
          nack.formattedMessage (), this);
    
    reopen ();
  }

  private void createFederationLink (String remoteServerDomain)
  {
    String remoteHost = remoteAddress.getHostName ();

    diagnostic ("Federation outgoing link established with " + 
                remoteHost + ", remote server domain \"" + 
                remoteServerDomain + "\"", this);
    
    link =
      new Link (session, router,
                          federationClass, serverDomain, 
                          remoteServerDomain, remoteHost);
  }
  
  private void send (Message message)
  {
    Federation.send (session, serverDomain, message);
  }
  
  // IoHandler  
  
  public void sessionOpened (IoSession theSession)
    throws Exception
  {
    diagnostic ("Federator \"" + serverDomain + 
                "\" connected to remote federator at " + uri, this);
  }
  
  public void sessionClosed (IoSession theSession)
    throws Exception
  {
    diagnostic ("Federator \"" + serverDomain + "\" disconnected from remote " +
    		"federator at " + uri, this);
    
    reopen ();
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
