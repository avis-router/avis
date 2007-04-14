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
import org.avis.net.messages.SubModRqst;
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

import static org.avis.net.client.ConnectionOptions.EMPTY_OPTIONS;
import static org.avis.net.client.SecureMode.ALLOW_INSECURE_DELIVERY;
import static org.avis.net.common.ElvinURI.defaultProtocol;
import static org.avis.net.security.Keys.EMPTY_KEYS;
import static org.avis.util.Text.className;

/**
 * Manages a client's connection to an Elvin router. Typically a
 * client creates a connection and then
 * {@linkplain #subscribe(String) subscribes} to notifications and/or
 * {@linkplain #send(Notification) sends} them.
 * <p>
 * 
 * <h3>Threading and synchrony notes</h3>
 * <p>
  *
 * <ul>
 * 
 * <li>This class is thread safe and may be accessed from any number
 * of client threads.
 * 
 * <li>All changes requiring a response from the router, such as
 * subscribing, are synchronous.
 * 
 * <li>Callbacks to the client initiated by a message from the
 * router, such as notifications, are done from a separate thread
 * managed by this connection. No guarantees on the callback thread
 * are given, in particular the same thread may not be used each time.
 * 
 * <li>Clients have full access to the connection during a callback:
 * changing subscriptions and sending notifications from a callback is
 * fully supported.
 * 
 * <li>While clients should try not to take a lot of time in a
 * callback, this connection will create extra callback threads if
 * needed if one callback takes too long.
 * </ul>
 * 
 * @author Matthew Phillips
 */
public final class Elvin
{
  private ElvinURI elvinUri;
  private IoSession clientSession;
  private ExecutorService executor;
  private boolean elvinConnectionOpen;
  private Map<Long, Subscription> subscriptions;
  private Keys notificationKeys;
  private Keys subscriptionKeys;
  private int receiveTimeout;
  
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
    this (elvinUri, EMPTY_OPTIONS, EMPTY_KEYS, EMPTY_KEYS);
  }
  
  /**
   * Create a new connection to an Elvin router.
   * 
   * @param elvinUri The URI of the router to connect to.
   * @param options The connection options.
   * @param notificationKeys The new notification keys. These
   *          automatically apply to all notifications, exactly as if
   *          they were added to the keys in the
   *          {@linkplain #send(Notification, SecureMode, Keys) send}
   *          call.
   * @param subscriptionKeys The new subscription keys. These
   *          automatically apply to all subscriptions, exactly as if
   *          they were added to the keys in the
   *          {@linkplain #subscribe(String, SecureMode, Keys) subscription},
   *          call. This includes currently existing subscriptions.
   * 
   * @throws IllegalArgumentException if one of the arguments is not
   *           valid.
   * @throws ConnectException if the socket to the router could not be
   *           opened, e.g. connection refused.
   * @throws ConnectionOptionsException if the router rejected the
   *           connection options. The client may elect to change the
   *           options and try to create a new connection.
   * 
   * @throws IOException if some other IO error occurs.
   * 
   * @see #subscribe(String, SecureMode, Keys)
   * @see #send(Notification, SecureMode, Keys)
   * @see #setKeys(Keys, Keys)
   */
  public Elvin (ElvinURI elvinUri, ConnectionOptions options,
                Keys notificationKeys, Keys subscriptionKeys)
    throws IllegalArgumentException, ConnectException,
           IOException, ConnectionOptionsException
  {
    this.elvinUri = elvinUri;
    this.notificationKeys = notificationKeys;
    this.subscriptionKeys = subscriptionKeys;
    this.subscriptions = new HashMap<Long, Subscription> ();
    this.executor = newCachedThreadPool ();
    this.replySemaphore = new Object ();
    this.receiveTimeout = 10000;
    
    if (!elvinUri.protocol.equals (defaultProtocol ()))
      throw new IllegalArgumentException
        ("Only the default protocol stack " +
         defaultProtocol () + " is currently supported");
    
    openConnection ();
    
    ConnRply connRply =
      sendAndReceive (new ConnRqst (elvinUri.versionMajor,
                                    elvinUri.versionMinor,
                                    options.asMap (),
                                    notificationKeys, subscriptionKeys),
                      ConnRply.class);
    
    elvinConnectionOpen = true;
    
    Map<String, Object> rejectedOptions =
      options.differenceFrom (connRply.options);
    
    if (!rejectedOptions.isEmpty ())
    {
      close ();
      
      throw new ConnectionOptionsException (options, rejectedOptions);
    }
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
      connectorConfig.setConnectTimeout (receiveTimeout);
      
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

      ConnectFuture connectFuture =
        connector.connect
          (new InetSocketAddress (elvinUri.host, elvinUri.port),
           new MessageHandler (), connectorConfig);
                                       
      connectFuture.join ();
      clientSession = connectFuture.getSession ();
    } catch (RuntimeIOException ex)
    {
      // unwrap MINA's RuntimeIOException
      throw (IOException)ex.getCause ();
    }
  }
  
  /** 
   * Close this connection. May be executed more than once.
   * 
   * @see #isOpen()
   */
  public synchronized void close ()
  {
    if (isOpen ())
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
      
      for (Subscription subscription : subscriptions.values ())
        subscription.id = 0;
      
      subscriptions = null;
    }
    
    if (executor != null)
    {
      executor.shutdown ();
      executor = null;
    }
  }

  /**
   * Test if this connection is open i.e. has not been
   * {@linkplain #close() closed}.
   * 
   * @see #close()
   */
  public synchronized boolean isOpen ()
  {
    return clientSession != null && clientSession.isConnected ();
  }
  
  /**
   * @see #setReceiveTimeout(int)
   */
  public int receiveTimeout ()
  {
    return receiveTimeout;
  }

  /**
   * Set the amount of time (in milliseconds) that must pass before
   * the router is assumed not to be responding. Default is 10
   * seconds (10,000 millis).
   */
  public void setReceiveTimeout (int receiveTimeout)
  {
    this.receiveTimeout = receiveTimeout;
  }
  
  /**
   * Return the mutex used to ensure this connection is thread safe.
   * All public methods on this connection that modify state or
   * otherwise need to be thread safe acquire this. Clients may choose
   * to pre-acquire this mutex to execute several operations
   * atomically -- see example in
   * {@link #subscribe(String, SecureMode, Keys)}.
   * <p>
   * 
   * NB: while this mutex is held, all callbacks (e.g. notification
   * delivery) will be blocked.
   */
  public Object mutex ()
  {
    return this;
  }
  
  /**
   * Create a new subscription with no security requirements. See
   * {@link #subscribe(String, SecureMode, Keys)} for details.
   * 
   * @param subscriptionExpr The subscription expression.
   * @return The subscription instance.
   * 
   * @throws IOException if an IO error occurs.
   */
  public Subscription subscribe (String subscriptionExpr)
    throws IOException
  {
    return subscribe (subscriptionExpr, EMPTY_KEYS);
  }
  
  /**
   * Create a new subscription with a given set of keys but allowing
   * insecure notifications. See
   * {@link #subscribe(String, SecureMode, Keys)} for details.
   * 
   * @param subscriptionExpr The subscription expression.
   * @return The subscription instance.
   * 
   * @throws IOException if an IO error occurs.
   */
  public Subscription subscribe (String subscriptionExpr,
                                 Keys keys)
    throws IOException
  {
    return subscribe (subscriptionExpr, ALLOW_INSECURE_DELIVERY, keys);
  }
  
  /**
   * Create a new subscription. The returned subscription instance can
   * be used to listen for notifications, modify subscription settings
   * and unsubscribe.
   * <p>
   * 
   * NB: there exists the possibility that between creating a
   * subscription and adding a listener to it, the client could miss a
   * notification. If this is a problem, the client may ensure this
   * will not happen by acquiring the connection's
   * {@linkplain #mutex() mutex} before subscribing. e.g.
   * 
   * <pre>
   *   Elvin elvin = ...;
   *   
   *   synchronized (elvin.mutex ())
   *   {
   *     Subscription sub = elvin.subscribe (...);
   *     
   *     sub.addNotificationListener (...);
   *   }
   * </pre>
   * 
   * @param subscriptionExpr The subscription expression to match
   *          notifications.
   * @param secureMode The security mode: specifying
   *          REQUIRE_SECURE_DELIVERY means the subscription will only
   *          receive notifications that are sent by clients with keys
   *          matching the set supplied here.
   * @param keys The keys that must match notificiation keys for
   *          secure delivery.
   * @return The subscription instance.
   * 
   * @throws IOException if an IO error occurs.
   * 
   * @see #send(Notification, SecureMode, Keys)
   * @see Subscription
   * @see SecureMode
   * @see Keys
   */
  public synchronized Subscription subscribe (String subscriptionExpr,
                                              SecureMode secureMode,
                                              Keys keys)
    throws IOException
  {
    Subscription subscription =
      new Subscription (this, subscriptionExpr, secureMode, keys);
    
    subscribe (subscription);
    
    return subscription;
  }
  
  private void subscribe (Subscription subscription)
    throws IOException
  {
    SubAddRqst subAddRqst = new SubAddRqst (subscription.subscriptionExpr);
    
    subAddRqst.acceptInsecure =
      subscription.secureMode == ALLOW_INSECURE_DELIVERY;
    subAddRqst.keys = subscription.keys;
    
    subscription.id =
      sendAndReceive (subAddRqst, SubRply.class).subscriptionId;
    
    subscriptions.put (subscription.id, subscription);
  }

  void unsubscribe (Subscription subscription)
    throws IOException
  {
    if (subscriptions.remove (subscription.id) != subscription)
      throw new IllegalArgumentException ("Not a valid subcription");

    sendAndReceive (new SubDelRqst (subscription.id), SubRply.class);
  }

  void modifyKeys (Subscription subscription, Keys newKeys)
    throws IOException
  {
    Delta<Keys> delta = subscription.keys.computeDelta (newKeys);
    
    sendAndReceive
      (new SubModRqst (subscription.id, delta.added, delta.removed), 
       SubRply.class);
  }

  void modifySubscriptionExpr (Subscription subscription,
                               String subscriptionExpr)
    throws IOException
  {
    sendAndReceive
      (new SubModRqst (subscription.id, subscriptionExpr), SubRply.class);
  }
  
  /**
   * Test if a given subscription is part of this connection.
   * 
   * @see Subscription#isActive()
   */
  public synchronized boolean hasSubscription (Subscription subscription)
  {
    return subscriptions.containsValue (subscription);
  }

  /**
   * Send a notification with no security requirements. See
   * {@link #send(Notification, SecureMode, Keys)} for details.
   */
  public void send (Notification notification)
    throws IOException
  {
    send (notification, EMPTY_KEYS);
  }
  
  /**
   * Send a notification with a set of keys but no requirement for
   * secure delivery. See
   * {@link #send(Notification, SecureMode, Keys)} for details.
   */
  public void send (Notification notification, Keys keys)
    throws IOException
  {
    send (notification, ALLOW_INSECURE_DELIVERY, keys);
  }
  
  /**
   * Send a notification with a specified secure mode. See
   * {@link #send(Notification, SecureMode, Keys)} for details.
   */
  public void send (Notification notification, SecureMode secureMode)
    throws IOException
  {
    send (notification, secureMode, EMPTY_KEYS);
  }

  /**
   * Send a notification.
   * 
   * @param notification The notification to send.
   * @param secureMode The security requirement.
   *          REQUIRE_SECURE_DELIVERY means the notification will only
   *          be received by subscriptions with keys matching the set
   *          supplied here.
   * @param keys The keys that must match for secure delivery.
   * 
   * @throws IOException if an IO error occurs.
   * 
   * @see #subscribe(String, SecureMode, Keys)
   * @see Notification
   * @see SecureMode
   * @see Keys
   */
  public synchronized void send (Notification notification,
                                 SecureMode secureMode,
                                 Keys keys)
    throws IOException
  {
    send (new NotifyEmit (notification.asMap (),
                          secureMode == ALLOW_INSECURE_DELIVERY, keys));
  }
  
  /**
   * Change the connection-wide keys used to secure receipt and
   * delivery of notifications.
   * 
   * @param newNotificationKeys The new notification keys. These
   *          automatically apply to all notifications, exactly as if
   *          they were added to the keys in the
   *          {@linkplain #send(Notification, SecureMode, Keys) send}
   *          call.
   * @param newSubscriptionKeys The new subscription keys. These
   *          automatically apply to all subscriptions, exactly as if
   *          they were added to the keys in the
   *          {@linkplain #subscribe(String, SecureMode, Keys) subscription},
   *          call. This includes currently existing subscriptions.
   * 
   * @throws IOException if an IO error occurs.
   */
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
  
  private void send (Message message)
    throws IOException
  {
    checkConnected ();
  
    if (isEnabled (TRACE))
      trace ("Client sent message: " + message, this);
  
    clientSession.write (message);
  }

  /**
   * Send a message and receive a reply with a matching transaction ID
   * and a given type.
   * 
   * @param <E> The reply message type.
   * @param request The request message.
   * @param expectedReplyType The expected reply type (same as E).
   * 
   * @return The reply message.
   * 
   * @throws IOException if no suitable reply is seen or an network
   *           error occurs.
   */
  private <E extends XidMessage> E sendAndReceive (XidMessage request,
                                                   Class<E> expectedReplyType)
    throws IOException
  {
    send (request);
   
    return receiveReply (request, expectedReplyType);
  }
  
  /**
   * Block the calling thread for up to receiveTimeout millis waiting
   * for a reply message to arrive from the router.
   */
  private XidMessage receiveReply ()
    throws IOException
  {
    synchronized (replySemaphore)
    {
      if (lastReply == null)
      {
        try
        {
          replySemaphore.wait (receiveTimeout);
        } catch (InterruptedException ex)
        {
          throw new RuntimeException (ex);
        }
      }
    
      if (lastReply != null)
      {
        XidMessage message = lastReply;
        
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

  /**
   * Block the calling thread for up to receiveTimeout millis waiting
   * for a reply message to arrive from the router.
   * 
   * @param <E> The reply message type.
   * @param request The request message.
   * @param expectedReplyType The expected reply type (same as E).
   * 
   * @return The reply message.
   * 
   * @throws IOException if no suitable reply is seen or an network
   *           error occurs.
   */
  @SuppressWarnings("unchecked")
  private <E extends XidMessage> E receiveReply (XidMessage request,
                                                 Class<E> expectedReplyType)
    throws IOException
  {
    XidMessage reply = receiveReply ();
    
    if (request.xid != reply.xid)
    {
      throw new IOException
        ("Protocol error: XID mismatch in reply from router");
    } else if (expectedReplyType.isAssignableFrom (reply.getClass ()))
    {
      return (E)reply;
    } else if (reply instanceof Nack)
    {
      Nack nack = (Nack)reply;
      
      // todo need to expand arg refs in nack error message from Mantara Elvin 
      // e.g. %1: Expression '%2' does not refer to a name
      throw new IOException
        ("Router rejected request: " + nack.errorText () +
         ": " + nack.message);
    } else
    {
      throw new IOException
        ("Protocol error: received a " + className (reply) +
         ": was expecting " + className (expectedReplyType));
    }
  }
  
  /**
   * Handle replies to client-initiated messages by delivering them
   * back to the waiting thread.
   */
  void handleReply (XidMessage reply)
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
  void handleMessage (Message message)
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
       
        synchronized (Elvin.this)
        {
          if (isOpen ())
          {
            fireNotify (message.secureMatches, true, ntfn);
            fireNotify (message.insecureMatches, false, ntfn);
          }
        }
      }
   });
  }

  protected void fireNotify (long [] subscriptionIds,
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
        warn ("Received notification for unknown subscription ID " +
              subscriptionId, this);
      }
    }
  }
  
  private void checkConnected ()
    throws IllegalStateException
  {
    if (clientSession == null || !clientSession.isConnected ())
      throw new IllegalStateException ("Not connected");
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
