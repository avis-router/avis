package org.avis.net.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import java.io.IOException;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.common.Notification;
import org.avis.net.common.ConnectionOptions;
import org.avis.net.common.ElvinURI;
import org.avis.net.common.FrameCodec;
import org.avis.net.messages.ConnRply;
import org.avis.net.messages.ConnRqst;
import org.avis.net.messages.Disconn;
import org.avis.net.messages.DisconnRply;
import org.avis.net.messages.DisconnRqst;
import org.avis.net.messages.Message;
import org.avis.net.messages.Nack;
import org.avis.net.messages.NotifyDeliver;
import org.avis.net.messages.NotifyEmit;
import org.avis.net.messages.SecRply;
import org.avis.net.messages.SecRqst;
import org.avis.net.messages.SubAddRqst;
import org.avis.net.messages.SubDelRqst;
import org.avis.net.messages.SubRply;
import org.avis.net.messages.XidMessage;
import org.avis.net.security.Keys;
import org.avis.util.Delta;

import dsto.dfc.logging.Log;

import static dsto.dfc.logging.Log.TRACE;
import static dsto.dfc.logging.Log.alarm;
import static dsto.dfc.logging.Log.isEnabled;
import static dsto.dfc.logging.Log.trace;
import static dsto.dfc.logging.Log.warn;

import static java.util.concurrent.Executors.newCachedThreadPool;

import static org.avis.common.Common.CLIENT_VERSION_MAJOR;
import static org.avis.common.Common.CLIENT_VERSION_MINOR;
import static org.avis.net.common.ElvinURI.defaultProtocol;
import static org.avis.net.security.Keys.EMPTY_KEYS;
import static org.avis.util.Text.className;

public class Elvin
{
  private static final int RECEIVE_TIMEOUT = 5000;
  
  private ElvinURI elvinUri;
  private IoSession clientSession;
  private ExecutorService executor;
  private boolean elvinConnectionOpen;
  private Map<Long, Subscription> subscriptions;
  private Keys notificationKeys;
  private Keys subscriptionKeys;
  
  /** This is effectively a single-item queue for handling responses
      to XID-based requests. It's volatile since it's used for inter-
      thread communication. */
  protected volatile XidMessage lastReply;
  private Object replySemaphore;
  
  public Elvin (String elvinUri)
    throws URISyntaxException, IllegalArgumentException,
           ConnectException, IOException
  {
    this (new ElvinURI (elvinUri));
  }
  
  public Elvin (ElvinURI elvinUri)
    throws IllegalArgumentException, ConnectException, IOException
  {
    this (elvinUri, new ConnectionOptions (),
          EMPTY_KEYS, EMPTY_KEYS);
  }
  
  public Elvin (ElvinURI elvinUri, ConnectionOptions options,
                Keys notificationKeys, Keys subscriptionKeys)
    throws IllegalArgumentException, ConnectException, IOException
  {
    this.elvinUri = elvinUri;
    this.notificationKeys = notificationKeys;
    this.subscriptionKeys = subscriptionKeys;
    this.subscriptions = new HashMap<Long, Subscription> ();
    this.executor = newCachedThreadPool ();
    this.replySemaphore = new Object ();
    
    if (!elvinUri.protocol.equals (defaultProtocol ()))
      throw new IllegalArgumentException
        ("Only the default protocol stack (" +
         defaultProtocol () + ") is currently supported");
    
    openConnection ();
    
    sendAndReceive (new ConnRqst (CLIENT_VERSION_MAJOR, CLIENT_VERSION_MINOR,
                                  options.asMap (),
                                  notificationKeys, subscriptionKeys),
                    ConnRply.class);
    
    elvinConnectionOpen = true;
    
    // todo check connection options
  }

  private void openConnection ()
    throws IOException
  {
    try
    {
      SocketConnector connector = new SocketConnector (1, executor);

      /* Change the worker timeout to 1 second to make the I/O thread
       * quit soon when there's no connection to manage. */
      connector.setWorkerTimeout (1);
      
      SocketConnectorConfig connectorConfig = new SocketConnectorConfig ();
      connectorConfig.setThreadModel (ThreadModel.MANUAL);
      connectorConfig.setConnectTimeout (10);
      
      DemuxingProtocolCodecFactory codecFactory =
        new DemuxingProtocolCodecFactory ();
      codecFactory.register (FrameCodec.class);
      
      DefaultIoFilterChainBuilder filterChainBuilder =
        connectorConfig.getFilterChain ();
      
      filterChainBuilder.addLast
        ("codec", new ProtocolCodecFilter (codecFactory));
      
      // below adds a thread pool to IO processor: probably don't need this
      // filterChainBuilder.addLast
      //   ("threadPool", new ExecutorFilter (executor));

      ConnectFuture future =
        connector.connect
          (new InetSocketAddress (elvinUri.host, elvinUri.port),
           new MessageHandler (), connectorConfig);
                                       
      future.join ();
      clientSession = future.getSession ();
    } catch (RuntimeIOException ex)
    {
      // unwrap MINA's RuntimeIOException
      throw (IOException)ex.getCause ();
    }
  }
  
  public synchronized void close ()
  {
    if (isConnected ())
    {
      if (elvinConnectionOpen)
      {
        try
        {
          sendAndReceive (new DisconnRqst (), DisconnRply.class);
        } catch (IOException ex)
        {
          Log.diagnostic ("Failed to cleanly disconnect", this, ex);
        } finally
        {
          elvinConnectionOpen = false;
        }
      }
      
      clientSession.close ();
      clientSession = null;
    }
    
    if (executor != null)
    {
      executor.shutdown ();
      executor = null;
    }
  }
  
  public synchronized boolean isConnected ()
  {
    return clientSession != null && clientSession.isConnected ();
  }
  
  public Subscription subscribe (String subscriptionExpr)
    throws IOException
  {
    Subscription sub = new Subscription (subscriptionExpr);
    
    subscribe (sub);
    
    return sub;
  }

  public synchronized void subscribe (Subscription subscription)
    throws IOException
  {
    SubAddRqst subAddRqst = new SubAddRqst (subscription.subscriptionExpr);
    subAddRqst.acceptInsecure = subscription.acceptInsecure;
    subAddRqst.keys = subscription.keys;
    
    subscription.id =
      sendAndReceive (subAddRqst, SubRply.class).subscriptionId;
    
    subscriptions.put (subscription.id, subscription);
  }
  
  public synchronized void unsubscribe (Subscription subscription)
    throws IOException
  {
    if (subscriptions.remove (subscription.id) != subscription)
      throw new IllegalArgumentException ("Not a valid subcription");

    sendAndReceive (new SubDelRqst (subscription.id), SubRply.class);
  }
  
  public synchronized boolean hasSubscription (Subscription subscription)
  {
    return subscriptions.containsValue (subscription);
  }

  public synchronized void send (Notification notification)
    throws IOException
  {
    send (new NotifyEmit (notification));
  }
  
  public synchronized void sendSecure (Notification notification)
    throws IOException
  {
    sendSecure (notification, EMPTY_KEYS);
  }
  
  public synchronized void sendSecure (Notification notification, Keys keys)
    throws IOException
  {
    send (new NotifyEmit (notification, false, keys));
  }
  
  public synchronized void setKeys (Keys newNotificationKeys,
                                    Keys newSubscriptionKeys)
    throws IOException
  {
    Delta<Keys> deltaNotificationKeys =
      notificationKeys.computeDelta (newNotificationKeys);
    Delta<Keys> deltaSubscriptionKeys =
      subscriptionKeys.computeDelta (newSubscriptionKeys);
    
    sendAndReceive
      (new SecRqst (deltaNotificationKeys.added, deltaNotificationKeys.removed,
                    deltaSubscriptionKeys.added, deltaSubscriptionKeys.removed),
       SecRply.class);
    
    this.notificationKeys = newNotificationKeys;
    this.subscriptionKeys = newSubscriptionKeys;
  }
  
  private <E extends Message> E sendAndReceive (XidMessage request,
                                                Class<E> expectedReplyType)
    throws IOException
  {
    send (request);
   
    return receive (expectedReplyType, request);
  }
  
  private void send (Message message)
    throws IOException
  {
    checkConnected ();

    if (isEnabled (TRACE))
      trace ("Client sent message: " + message, this);

    clientSession.write (message);
  }
  
  private Message receive (long timeout)
    throws IOException
  {
    synchronized (replySemaphore)
    {
      if (lastReply == null)
      {
        try
        {
          replySemaphore.wait (timeout);
        } catch (InterruptedException ex)
        {
          throw new RuntimeException (ex);
        }
      }
    
      if (lastReply != null)
      {
        Message message = lastReply;
        
        lastReply = null;
        
        return message;
      } else
      {
        // may have failed because we are simply not connected any more
        checkConnected ();
        
        throw new IOException ("Timeout error: did not receive a reply");
      }
    }
  }
  
  private void checkConnected ()
    throws IllegalStateException
  {
    if (!clientSession.isConnected ())
      throw new IllegalStateException ("Not connected");
  }
  
  private <E extends Message> E receive (Class<E> expectedMessageType,
                                         XidMessage inResponseTo)
    throws IOException
  {
    return receive (expectedMessageType, inResponseTo, RECEIVE_TIMEOUT);
  }
  
  @SuppressWarnings("unchecked")
  private <E extends Message> E receive (Class<E> expectedMessageType,
                                         XidMessage inResponseTo,
                                         long timeout)
    throws IOException
  {
    Message message = receive (timeout);
      
    if (expectedMessageType.isAssignableFrom (message.getClass ()))
    {
      if (inResponseTo != null && inResponseTo.xid != ((XidMessage)message).xid)
        throw new IllegalStateException ("XID mismatch");
      
      return (E)message;
    } else if (message instanceof Nack)
    {
      Nack nack = (Nack)message;
      
      // todo need to expand arg refs in nack error message from Mantara Elvin 
      // e.g. %1: Expression '%2' does not refer to a name
      throw new IOException
        ("Router rejected request: " + nack.errorText () +
         ": " + nack.message);
    } else
    {
      throw new IllegalStateException
        ("Received a " + className (message) +
         ": was expecting " + className (expectedMessageType));
    }
  }
  
  /**
   * Handle replies to client-initiated messages by delivering them
   * back to the waiting thread.
   */
  protected void handleReply (XidMessage reply)
  {
    synchronized (replySemaphore)
    {
      if (lastReply != null)
        throw new IllegalStateException ("Reply buffer overflow");

      lastReply = reply;
      replySemaphore.notifyAll ();
    }
  }
  
  /**
   * Handle router-initiated messages (e.g. NotifyDeliver, Disconn, etc).
   */
  protected void handleMessage (Message message)
  {
    switch (message.typeId ())
    {
      case NotifyDeliver.ID:
        handleNotifyDeliver ((NotifyDeliver)message);
        break;
      case Disconn.ID:
        handleDisconnect ((Disconn)message);
        break;
      default:
        alarm ("Unexpected server message: " + message, this);
    }
  }

  private synchronized void handleDisconnect (Disconn disconn)
  {
    elvinConnectionOpen = false;
    
    close ();
  }
  
  private void handleNotifyDeliver (final NotifyDeliver message)
  {
    /*
     * Do not fire event in this thread since a listener may trigger a
     * receive () which will block the IO processor thread by waiting
     * for a reply that cannot be processed since the IO processor is
     * busy calling this method.
     */
    executor.execute (new Runnable ()
    {
      public void run ()
      {
        Notification ntfn = new Notification (message.attributes);
        
        fireNotify (message.secureMatches, true, ntfn);
        fireNotify (message.insecureMatches, false, ntfn);
      }
   });
  }

  protected synchronized void fireNotify (long [] subscriptionIds,
                                          boolean secure,
                                          Notification ntfn)
  {
    for (long subscriptionId : subscriptionIds)
    {
      Subscription subscription = subscriptions.get (subscriptionId);
      
      if (subscription != null)
      {
        NotificationEvent event =
          new NotificationEvent (this, subscription, ntfn, secure);
        
        subscription.notifyListeners (event);
      } else
      {
        warn ("Received notification for unknown subscription " +
              "(" + subscriptionId + ")", this);
      }
    }
  }
  
  class MessageHandler extends IoHandlerAdapter
  {
    public void exceptionCaught (IoSession session, Throwable cause)
      throws Exception
    {
      alarm ("Unexpected exception in Elvin client", this, cause);
    }

    public void messageReceived (IoSession session, Object message)
      throws Exception
    {
      if (isEnabled (TRACE))
        trace ("Client got message: " + message, this);
      
      if (message instanceof XidMessage)
        handleReply ((XidMessage)message);
      else
        handleMessage ((Message)message);
    }
    
    @Override
    public void sessionClosed (IoSession session)
      throws Exception
    {
      close ();
    }
  }
}
