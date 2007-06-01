package org.avis.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import java.io.Closeable;
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

import org.avis.common.ElvinURI;
import org.avis.io.FrameCodec;
import org.avis.io.messages.ConnRply;
import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.DisconnRqst;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.NotifyDeliver;
import org.avis.io.messages.NotifyEmit;
import org.avis.io.messages.RequestMessage;
import org.avis.io.messages.SecRqst;
import org.avis.io.messages.SubAddRqst;
import org.avis.io.messages.SubDelRqst;
import org.avis.io.messages.SubModRqst;
import org.avis.io.messages.XidMessage;
import org.avis.security.Keys;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static org.avis.client.ConnectionOptions.EMPTY_OPTIONS;
import static org.avis.client.SecureMode.ALLOW_INSECURE_DELIVERY;
import static org.avis.common.ElvinURI.defaultProtocol;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.util.Text.className;

import static dsto.dfc.logging.Log.TRACE;
import static dsto.dfc.logging.Log.alarm;
import static dsto.dfc.logging.Log.diagnostic;
import static dsto.dfc.logging.Log.isEnabled;
import static dsto.dfc.logging.Log.trace;
import static dsto.dfc.logging.Log.warn;

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
 * @todo add liveness test
 * @todo think about global notification listener support
 * 
 * @author Matthew Phillips
 */
public final class Elvin implements Closeable
{
  private ElvinURI routerUri;
  private ConnectionOptions connectionOptions;
  private IoSession clientSession;
  private boolean elvinConnectionOpen;
  private Map<Long, Subscription> subscriptions;
  private Keys notificationKeys;
  private Keys subscriptionKeys;
  private int receiveTimeout;

  /**
   * We use a multiple-thread IO pool and a single-thread callback
   * pool. Using the IO pool for callbacks results in a lot of threads
   * being spawned when a sudden influx of notifications come in.
   */
  private ExecutorService ioExecutor;
  private ExecutorService callbackExecutor;

  /** lastReply is effectively a single-item queue for handling responses
      to XID-based requests, using replySemaphore to synchronize access. */
  private XidMessage lastReply;
  private Object replySemaphore;
  
  /**
   * Create a new connection to an Elvin router.
   * 
   * @param elvinUri A URI for the Elvin router.
   * 
   * @throws URISyntaxException if elvinUri is invalid.
   * @throws IllegalArgumentException if one of the arguments is not
   *           valid.
   * @throws ConnectException if the socket to the router could not be
   *           opened, e.g. connection refused.
   * @throws IOException if a general network error occurs.
   * 
   * @see #Elvin(ElvinURI, ConnectionOptions, Keys, Keys)
   */
  public Elvin (String elvinUri)
    throws URISyntaxException,
           IllegalArgumentException,
           ConnectException,
           IOException
  {
    this (new ElvinURI (elvinUri));
  }
  
  /**
   * Create a new connection to an Elvin router.
   * 
   * @param routerUri A URI for the Elvin router.
   * 
   * @throws IllegalArgumentException if one of the arguments is not
   *           valid.
   * @throws ConnectException if the socket to the router could not be
   *           opened, e.g. connection refused.
   * @throws IOException if a general network error occurs.
   * 
   * @see #Elvin(ElvinURI, ConnectionOptions, Keys, Keys)
   */
  public Elvin (ElvinURI routerUri)
    throws IllegalArgumentException, ConnectException, IOException
  {
    this (routerUri, EMPTY_OPTIONS, EMPTY_KEYS, EMPTY_KEYS);
  }
  
  /**
   * Create a new connection to an Elvin router.
   * 
   * @param routerUri A URI for the Elvin router.
   * @param options The connection options.
   * 
   * @throws IllegalArgumentException if one of the arguments is not
   *           valid.
   * @throws ConnectException if the socket to the router could not be
   *           opened, e.g. connection refused.
   * @throws IOException if a general network error occurs.
   * 
   * @see #Elvin(ElvinURI, ConnectionOptions, Keys, Keys)
   */
  public Elvin (ElvinURI routerUri, ConnectionOptions options)
    throws IllegalArgumentException,
           ConnectException,
           IOException,
           ConnectionOptionsException
  {
    this (routerUri, options, EMPTY_KEYS, EMPTY_KEYS);
  }
  
  /**
   * Create a new connection to an Elvin router.
   * 
   * @param routerUri The URI of the router to connect to.
   * @param options The connection options.
   * @param notificationKeys These keys automatically apply to all
   *          notifications, exactly as if they were added to the keys
   *          in the
   *          {@linkplain #send(Notification, SecureMode, Keys) send}
   *          call.
   * @param subscriptionKeys These keys automatically apply to all
   *          subscriptions, exactly as if they were added to the keys
   *          in the
   *          {@linkplain #subscribe(String, SecureMode, Keys) subscription},
   *          call.
   * 
   * @throws IllegalArgumentException if one of the arguments is not
   *           valid.
   * @throws ConnectException if a socket to the router could not be
   *           opened, e.g. connection refused.
   * @throws ConnectionOptionsException if the router rejected the
   *           connection options. The client may elect to change the
   *           options and try to create a new connection.
   * @throws IOException if some other IO error occurs.
   * 
   * @see #subscribe(String, SecureMode, Keys)
   * @see #send(Notification, SecureMode, Keys)
   * @see #setKeys(Keys, Keys)
   */
  public Elvin (ElvinURI routerUri, ConnectionOptions options,
                Keys notificationKeys, Keys subscriptionKeys)
    throws IllegalArgumentException,
           ConnectException,
           IOException,
           ConnectionOptionsException
  {
    this.routerUri = routerUri;
    this.connectionOptions = options;
    this.notificationKeys = notificationKeys;
    this.subscriptionKeys = subscriptionKeys;
    this.subscriptions = new HashMap<Long, Subscription> ();
    this.ioExecutor = newCachedThreadPool ();
    this.callbackExecutor = newSingleThreadExecutor ();
    this.replySemaphore = new Object ();
    this.receiveTimeout = 10000;
    
    if (!routerUri.protocol.equals (defaultProtocol ()))
      throw new IllegalArgumentException
        ("Only the default protocol stack " +
         defaultProtocol () + " is currently supported");
    
    openConnection ();
    
    ConnRply connRply =
      sendAndReceive (new ConnRqst (routerUri.versionMajor,
                                    routerUri.versionMinor,
                                    options.asMap (),
                                    notificationKeys, subscriptionKeys));
    
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
      SocketConnector connector = new SocketConnector (1, ioExecutor);

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
          (new InetSocketAddress (routerUri.host, routerUri.port),
           new MessageHandler (), connectorConfig);
                        
      // todo how to handle auto reconnect?
      if (!connectFuture.join (receiveTimeout))
        throw new IOException ("Timed out connecting to router " + routerUri);
      
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
          sendAndReceive (new DisconnRqst ());
        } catch (Exception ex)
        {
          diagnostic ("Failed to cleanly disconnect", this, ex);
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
    
    ioExecutor.shutdown ();
    callbackExecutor.shutdown ();
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
   * The router's URI.
   */
  public ElvinURI routerUri ()
  {
    return routerUri;
  }
  
  /**
   * The connection options established with the router. These cannot
   * be changed after connection.
   * 
   * @see #Elvin(ElvinURI, ConnectionOptions, Keys, Keys)
   */
  public ConnectionOptions connectionOptions ()
  {
    return connectionOptions;
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
  public synchronized void setReceiveTimeout (int receiveTimeout)
  {
    this.receiveTimeout = receiveTimeout;
  }
  
  /**
   * Return the mutex used to synchronize access to this connection.
   * All public methods on this connection that modify state or
   * otherwise need to be thread safe acquire this before operation.
   * Clients may choose to pre-acquire this mutex to execute several
   * operations atomically -- see example in
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
   * Create a new subscription. See
   * {@link #subscribe(String, SecureMode, Keys)} for details.
   * 
   * @param subscriptionExpr The subscription expression.
   * 
   * @return The subscription instance.
   * 
   * @throws IOException if an IO error occurs.
   */
  public Subscription subscribe (String subscriptionExpr)
    throws IOException
  {
    return subscribe (subscriptionExpr, ALLOW_INSECURE_DELIVERY, EMPTY_KEYS);
  }
  
  /**
   * Create a new subscription with a given set of security keys to
   * enable secure delivery, but also allowing insecure notifications.
   * See {@link #subscribe(String, SecureMode, Keys)} for details.
   * 
   * @param subscriptionExpr The subscription expression.
   * @return The subscription instance.
   * 
   * @throws IOException if an IO error occurs.
   */
  public Subscription subscribe (String subscriptionExpr, Keys keys)
    throws IOException
  {
    return subscribe (subscriptionExpr, ALLOW_INSECURE_DELIVERY, keys);
  }
  
  /**
   * Create a new subscription with a given security mode but with an
   * empty key set. Be careful when using REQUIRE_SECURE_DELIVERY with
   * this subscription option: if you don't specify keys for the
   * subscription elsewhere, either via {@link #setKeys(Keys, Keys)}
   * or {@link Subscription#setKeys(Keys)}, the subscription will
   * never be able to receive notifications.
   * <p>
   * 
   * See {@link #subscribe(String, SecureMode, Keys)} for more details.
   * 
   * @param subscriptionExpr The subscription expression.
   * @param secureMode The security mode: specifying
   *          REQUIRE_SECURE_DELIVERY means the subscription will only
   *          receive notifications that are sent by clients with keys
   *          matching the set supplied here or the global
   *          subscription key set.
   * @return The subscription instance.
   * 
   * @throws IOException if an IO error occurs.
   */
  public Subscription subscribe (String subscriptionExpr, SecureMode secureMode)
    throws IOException
  {
    return subscribe (subscriptionExpr, secureMode, EMPTY_KEYS);
  }
  
  /**
   * Create a new subscription. The returned subscription instance can
   * be used to listen for notifications, modify subscription settings
   * and unsubscribe.
   * <p>
   * 
   * NB: there exists the possibility that, between creating a
   * subscription and adding a listener to it, the client could miss a
   * notification. If necessary, the client may ensure this will not
   * happen by acquiring the connection's {@linkplain #mutex() mutex}
   * before subscribing. e.g.
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
   *          matching the set supplied here or the global subscription
   *          key set.
   * @param keys The keys that must match notificiation keys for
   *          secure delivery.
   * @return The subscription instance.
   * 
   * @throws IOException if an network error occurs.
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
  
  void subscribe (Subscription subscription)
    throws IOException
  {
    subscription.id =
      sendAndReceive
        (new SubAddRqst (subscription.subscriptionExpr,
                         subscription.keys,
                         subscription.acceptInsecure ())).subscriptionId;
    
    if (subscriptions.put (subscription.id, subscription) != null)
      throw new IOException
        ("Protocol error: server issued duplicate subscription ID " +
         subscription.id);
  }

  void unsubscribe (Subscription subscription)
    throws IOException
  {
    if (subscriptions.remove (subscription.id) != subscription)
      throw new IllegalStateException
        ("Internal error: invalid subscription ID " + subscription.id);

    sendAndReceive (new SubDelRqst (subscription.id));
  }

  void modifyKeys (Subscription subscription, Keys newKeys)
    throws IOException
  {
    Keys.Delta delta = subscription.keys.deltaFrom (newKeys);
    
    if (!delta.isEmpty ())
    {
      sendAndReceive
        (new SubModRqst (subscription.id, delta.added, delta.removed,
                         subscription.acceptInsecure ()));
    }
  }

  void modifySubscriptionExpr (Subscription subscription,
                               String subscriptionExpr)
    throws IOException
  {
    sendAndReceive
      (new SubModRqst (subscription.id, subscriptionExpr,
                       subscription.acceptInsecure ()));
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
   * Send a notification. See
   * {@link #send(Notification, SecureMode, Keys)} for details.
   */
  public void send (Notification notification)
    throws IOException
  {
    send (notification, ALLOW_INSECURE_DELIVERY, EMPTY_KEYS);
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
   * Send a notification with a specified security mode. Be careful
   * when using REQUIRE_SECURE_DELIVERY with this method: if you
   * haven't specified any global notification keys via
   * {@link #setKeys(Keys, Keys)} or
   * {@link #setNotificationKeys(Keys)}, the notification will never
   * be able to to be delivered.
   * <p>
   * See {@link #send(Notification, SecureMode, Keys)} for details.
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
   *          REQUIRE_SECURE_DELIVERY means the notification can only
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
   * @see #setNotificationKeys(Keys)
   */
  public Keys notificationKeys ()
  {
    return notificationKeys;
  }
  
  /**
   * Set the connection-wide notification keys used to secure delivery
   * of notifications.
   * 
   * @param newNotificationKeys The new notification keys. These
   *          automatically apply to all notifications, exactly as if
   *          they were added to the keys in the
   *          {@linkplain #send(Notification, SecureMode, Keys) send}
   *          call.
   * 
   * @throws IOException if an IO error occurs.
   * 
   * @see #setSubscriptionKeys(Keys)
   * @see #setKeys(Keys, Keys)
   */
  public synchronized void setNotificationKeys (Keys newNotificationKeys)
    throws IOException
  {
    setKeys (newNotificationKeys, subscriptionKeys);
  }
  
  /**
   * @see #setSubscriptionKeys(Keys)
   */
  public Keys subscriptionKeys ()
  {
    return notificationKeys;
  }
  
  /**
   * Set the connection-wide subscription keys used to secure receipt
   * of notifications.
   * 
   * @param newSubscriptionKeys The new subscription keys. These
   *          automatically apply to all subscriptions, exactly as if
   *          they were added to the keys in the
   *          {@linkplain #subscribe(String, SecureMode, Keys) subscription},
   *          call. This includes currently existing subscriptions.
   * 
   * @throws IOException if an IO error occurs.
   * 
   * @see #setNotificationKeys(Keys)
   * @see #setKeys(Keys, Keys)
   */
  public synchronized void setSubscriptionKeys (Keys newSubscriptionKeys)
    throws IOException
  {
    setKeys (notificationKeys, newSubscriptionKeys);
  }
  
  /**
   * Change the connection-wide keys used to secure the receipt and
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
   *          call. This applies to all existing and future
   *          subscriptions.
   * 
   * @throws IOException if an IO error occurs.
   */
  public synchronized void setKeys (Keys newNotificationKeys,
                                    Keys newSubscriptionKeys)
    throws IOException
  {
    Keys.Delta deltaNotificationKeys =
      notificationKeys.deltaFrom (newNotificationKeys);
    Keys.Delta deltaSubscriptionKeys =
      subscriptionKeys.deltaFrom (newSubscriptionKeys);

    if (!deltaNotificationKeys.isEmpty () || !deltaSubscriptionKeys.isEmpty ())
    {
      sendAndReceive
        (new SecRqst
           (deltaNotificationKeys.added, deltaNotificationKeys.removed,
            deltaSubscriptionKeys.added, deltaSubscriptionKeys.removed));
      
      this.notificationKeys = newNotificationKeys;
      this.subscriptionKeys = newSubscriptionKeys;
    }
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
   * Send a request message and receive a reply with the correct type
   * and transaction ID.
   * 
   * @param request The request message.
   * 
   * @return The reply message.
   * 
   * @throws IOException if no suitable reply is seen or a network
   *           error occurs.
   */
  private <R extends XidMessage> R sendAndReceive (RequestMessage<R> request)
    throws IOException
  {
    send (request);
   
    return receiveReply (request);
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
   * @param request The request message.
   * 
   * @return The reply message.
   * 
   * @throws IOException if no suitable reply is seen or an network
   *           error occurs.
   */
  @SuppressWarnings("unchecked")
  private <R extends XidMessage> R receiveReply (RequestMessage<R> request)
    throws IOException
  {
    XidMessage reply = receiveReply ();
    
    if (reply.xid != request.xid)
    {
      throw new IOException
        ("Protocol error: Transaction ID mismatch in reply from router");
    } else if (request.replyType ().isAssignableFrom (reply.getClass ()))
    {
      return (R)reply;
    } else if (reply instanceof Nack)
    {
      Nack nack = (Nack)reply;
      
      // todo need to expand arg refs in nack error message from Mantara Elvin 
      // e.g. %1: Expression '%2' does not refer to a name
      throw new IOException
        ("Router rejected request: " + nack.errorText () + ": " + nack.message);
    } else
    {
      // todo this indicates a pretty serious fuckup. should try to reconnect?
      throw new IOException
        ("Protocol error: received a " + className (reply) +
         ": was expecting " + className (request.replyType ()));
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
      replySemaphore.notify ();
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
    callbackExecutor.execute (new Runnable ()
    {
      public void run ()
      {
        Notification ntfn = new Notification (message);
       
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
        subscription.notifyListeners
          (new NotificationEvent (this, subscription, ntfn, secure));
      } else
      {
        warn ("Received notification for unknown subscription ID " +
              subscriptionId, this);
      }
    }
  }
  
  private void checkConnected ()
    throws IOException
  {
    IoSession session = clientSession;
    
    if (session == null)
      throw new IOException ("Connection is closed");
    else if (!session.isConnected ())
      throw new IOException ("Cannot operate while not connected to router");
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
