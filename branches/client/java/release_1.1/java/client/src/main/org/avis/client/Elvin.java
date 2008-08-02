package org.avis.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.Closeable;
import java.io.IOException;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.nio.channels.UnresolvedAddressException;

import javax.net.ssl.SSLException;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.common.ElvinURI;
import org.avis.common.InvalidURIException;
import org.avis.common.RuntimeInterruptedException;
import org.avis.io.ClientFrameCodec;
import org.avis.io.LivenessFilter;
import org.avis.io.messages.ConnRply;
import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.DisconnRqst;
import org.avis.io.messages.DropWarn;
import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.LivenessFailureMessage;
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
import org.avis.util.ListenerList;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Collections.emptySet;
import static java.util.concurrent.Executors.newCachedThreadPool;

import static org.avis.client.CloseEvent.REASON_CLIENT_SHUTDOWN;
import static org.avis.client.CloseEvent.REASON_IO_ERROR;
import static org.avis.client.CloseEvent.REASON_PROTOCOL_VIOLATION;
import static org.avis.client.CloseEvent.REASON_ROUTER_SHUTDOWN;
import static org.avis.client.CloseEvent.REASON_ROUTER_SHUTDOWN_UNEXPECTEDLY;
import static org.avis.client.CloseEvent.REASON_ROUTER_STOPPED_RESPONDING;
import static org.avis.client.ConnectionOptions.EMPTY_OPTIONS;
import static org.avis.client.ConnectionOptions.convertLegacyToNew;
import static org.avis.client.ElvinLogEvent.Type.DIAGNOSTIC;
import static org.avis.client.ElvinLogEvent.Type.ERROR;
import static org.avis.client.ElvinLogEvent.Type.WARNING;
import static org.avis.client.SecureMode.ALLOW_INSECURE_DELIVERY;
import static org.avis.common.ElvinURI.defaultProtocol;
import static org.avis.common.ElvinURI.secureProtocol;
import static org.avis.io.Net.enableTcpNoDelay;
import static org.avis.io.TLS.sslContextFor;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.util.Text.className;
import static org.avis.util.Util.checkNotNull;

/**
 * The core class in the Avis client library which manages a client's
 * connection to an Elvin router. Typically a client creates a
 * connection and then
 * {@linkplain #subscribe(String, Keys, SecureMode) subscribes} to
 * notifications and/or
 * {@linkplain #send(Notification, Keys, SecureMode) sends} them.
 * <p>
 * Example:
 * 
 * <pre>
 * Elvin elvin = new Elvin (&quot;elvin://elvinhostname&quot;);
 * 
 * // subscribe to some notifications
 * elvin.subscribe (&quot;Foo == 42 || begins-with (Bar, 'baz')&quot;);
 * 
 * // add a handler for notifications
 * elvin.addSubscriptionListener (new GeneralSubscriptionListener ()
 * {
 *   public void notificationReceived (GeneralNotificationEvent e)
 *   {
 *     System.out.println (&quot;Got a notification:\n&quot; + e.notification);
 *   }
 * });
 * 
 * // send a notification that we will get back from the router
 * Notification notification = new Notification ();
 * notification.set (&quot;Foo&quot;, 42);
 * notification.set (&quot;Bar&quot;, &quot;bar&quot;);
 * notification.set (&quot;Data&quot;, new byte []{0x00, 0xff});
 * 
 * elvin.send (notification);
 * 
 * elvin.close ();
 * </pre>
 * 
 * <p>
 * 
 * <h3>Threading And Synchronisation Notes</h3>
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
 * <li>Clients should not take a lot of time in a callback since all
 * other operations are blocked during this time (the client
 * {@linkplain #mutex() mutex} is automatically pre-acquired during
 * callback execution in order to avoid possible deadlock in callbacks
 * that access the client connection). Callbacks should execute long
 * running or blocking operations on another thread, e.g. via an
 * {@link ExecutorService}.  </ul>
 * 
 * @author Matthew Phillips
 */
public final class Elvin implements Closeable
{
  protected ElvinURI routerUri;
  protected ElvinOptions options;
  protected IoSession connection;
  protected AtomicBoolean connectionOpen;
  protected boolean elvinSessionEstablished;
  protected Map<Long, Subscription> subscriptions;
  protected Callbacks callbacks;
  
  /**
   * A multi-thread pool for handling MINA I/O.
   */
  protected ExecutorService ioExecutor;

  /**
   * lastReply is effectively a single-item queue for handling
   * responses to XID-based requests, using replyLock to synchronize
   * access.
   */
  protected XidMessage lastReply;
  protected Object replyLock;
  
  protected ListenerList<CloseListener> closeListeners;
  protected ListenerList<GeneralNotificationListener> notificationListeners;
  protected ListenerList<ElvinLogListener> logListeners;
  
  /**
   * Create a new connection to an Elvin router.
   * 
   * @param elvinUri A URI for the Elvin router.
   * 
   * @throws InvalidURIException if elvinUri is invalid.
   * @throws IllegalArgumentException if one of the arguments is not
   *           valid.
   * @throws ConnectException if the socket to the router could not be
   *           opened, e.g. connection refused.
   * @throws IOException if a general network error occurs.
   * 
   * @see #Elvin(ElvinURI, ConnectionOptions, Keys, Keys)
   */
  public Elvin (String elvinUri)
    throws InvalidURIException,
           IllegalArgumentException,
           ConnectException,
           IOException
  {
    this (new ElvinURI (elvinUri));
  }
  
  /**
   * Create a new connection to an Elvin router.
   * 
   * @param elvinUri A URI for the Elvin router.
   * @param options The connection options.
   * 
   * @throws InvalidURIException if elvinUri is invalid.
   * @throws IllegalArgumentException if one of the arguments is not
   *           valid.
   * @throws ConnectException if the socket to the router could not be
   *           opened, e.g. connection refused.
   * @throws IOException if a general network error occurs.
   * 
   * @see #Elvin(ElvinURI, ConnectionOptions, Keys, Keys)
   */
  public Elvin (String elvinUri, ConnectionOptions options)
    throws InvalidURIException,
           IllegalArgumentException,
           ConnectException,
           IOException
  {
    this (new ElvinURI (elvinUri), options);
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
   * @throws ConnectionOptionsException if the router rejects a
   *           connection option.
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
   * @param notificationKeys These keys automatically apply to all
   *          notifications, exactly as if they were added to the keys
   *          in the
   *          {@linkplain #send(Notification, Keys, SecureMode) send}
   *          call.
   * @param subscriptionKeys These keys automatically apply to all
   *          subscriptions, exactly as if they were added to the keys
   *          in the
   *          {@linkplain #subscribe(String, Keys, SecureMode) subscription}
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
   * @see #Elvin(ElvinURI, ConnectionOptions, Keys, Keys)
   */
  public Elvin (ElvinURI routerUri,
                Keys notificationKeys, Keys subscriptionKeys)
    throws IllegalArgumentException,
           ConnectException,
           IOException,
           ConnectionOptionsException
  {
    this (routerUri, EMPTY_OPTIONS, notificationKeys, subscriptionKeys);
  }

  /**
   * Create a new connection to an Elvin router.
   * 
   * @param routerUri The URI of the router to connect to.
   * @param options The connection options.
   * @param notificationKeys These keys automatically apply to all
   *          notifications, exactly as if they were added to the keys
   *          in the
   *          {@linkplain #send(Notification, Keys, SecureMode) send}
   *          call.
   * @param subscriptionKeys These keys automatically apply to all
   *          subscriptions, exactly as if they were added to the keys
   *          in the
   *          {@linkplain #subscribe(String, Keys, SecureMode) subscription}
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
   * @see #subscribe(String, Keys, SecureMode)
   * @see #send(Notification, Keys, SecureMode)
   * @see #setKeys(Keys, Keys)
   */
  public Elvin (ElvinURI routerUri, ConnectionOptions options,
                Keys notificationKeys, Keys subscriptionKeys)
    throws IllegalArgumentException,
           ConnectException,
           IOException,
           ConnectionOptionsException
  {
    this (routerUri, 
          new ElvinOptions (options, notificationKeys, subscriptionKeys));
  }
  
  /**
   * Create a new connection to an Elvin router.
   * 
   * @param routerUri The URI of the router to connect to.
   * @param options The Elvin client options. Modifying these after
   *                they have been passed into this constructor has no
   *                effect.
   * 
   * @throws IllegalArgumentException if one of the arguments is not
   *                 valid.
   * @throws ConnectException if a socket to the router could not be
   *                 opened, e.g. connection refused.
   * @throws ConnectionOptionsException if the router rejected the
   *                 connection options. The client may elect to
   *                 change the options and try to create a new
   *                 connection.
   * @throws IOException if some other IO error occurs.
   * 
   * @see #subscribe(String, Keys, SecureMode)
   * @see #send(Notification, Keys, SecureMode)
   * @see #setKeys(Keys, Keys)
   */
  public Elvin (String routerUri, ElvinOptions options)
    throws IllegalArgumentException,
           ConnectException,
           IOException,
           ConnectionOptionsException
  {
    this (new ElvinURI (routerUri), options);
  }
  
  /**
   * Create a new connection to an Elvin router.
   * 
   * @param routerUri The URI of the router to connect to.
   * @param options The Elvin client options. Modifying these after
   *                they have been passed into this constructor has no
   *                effect.
   * 
   * @throws IllegalArgumentException if one of the arguments is not
   *                 valid.
   * @throws ConnectException if a socket to the router could not be
   *                 opened, e.g. connection refused.
   * @throws ConnectionOptionsException if the router rejected the
   *                 connection options. The client may elect to
   *                 change the options and try to create a new
   *                 connection.
   * @throws IOException if some other IO error occurs.
   * 
   * @see #subscribe(String, Keys, SecureMode)
   * @see #send(Notification, Keys, SecureMode)
   * @see #setKeys(Keys, Keys)
   */
  public Elvin (ElvinURI routerUri, ElvinOptions options)
    throws IllegalArgumentException,
           ConnectException,
           IOException,
           ConnectionOptionsException
  {
    this.routerUri = routerUri;
    this.options = options.clone ();
    this.connectionOpen = new AtomicBoolean (true);
    this.subscriptions = new HashMap<Long, Subscription> ();
    this.closeListeners =
      new ListenerList<CloseListener> (CloseListener.class, 
                                       "connectionClosed", CloseEvent.class);
    this.notificationListeners =
      new ListenerList<GeneralNotificationListener> 
        (GeneralNotificationListener.class, "notificationReceived", 
         GeneralNotificationEvent.class);
    this.logListeners =
      new ListenerList<ElvinLogListener> 
        (ElvinLogListener.class, "messageLogged", ElvinLogEvent.class);
    
    this.replyLock = new Object ();
    this.callbacks = new Callbacks (this);
    
    if (!routerUri.protocol.equals (defaultProtocol ()) &&
        !routerUri.protocol.equals (secureProtocol ()))
    {
      throw new IllegalArgumentException
        ("Avis only supports protocols: " + 
         defaultProtocol () + " and " + secureProtocol () + 
         ": " + routerUri);
    }
    
    this.ioExecutor = newCachedThreadPool ();
    
    boolean successfullyConnected = false;
    
    try
    {
      openConnection ();      

      ConnRply connRply =
        sendAndReceive 
          (new ConnRqst (routerUri.versionMajor,
                         routerUri.versionMinor,
                         options.connectionOptions.asMapWithLegacy (),
                         options.notificationKeys, 
                         options.subscriptionKeys));
      
      elvinSessionEstablished = true;
      
      Map<String, Object> acceptedOptions = 
        convertLegacyToNew (connRply.options);
      
      Map<String, Object> rejectedOptions =
        options.connectionOptions.differenceFrom (acceptedOptions);
      
      if (!rejectedOptions.isEmpty ())
      {
        throw new ConnectionOptionsException 
          (options.connectionOptions, rejectedOptions);
      }

      // include any options the router has added that we didn't specify
      options.updateConnectionOptions (acceptedOptions);
      
      successfullyConnected = true;
    } finally
    {
      if (!successfullyConnected)
        close ();
    }
  }
  
  /**
   * Open a network connection to the router.
   */
  private void openConnection ()
    throws IOException, IllegalArgumentException
  {
    try
    {
      SocketConnector connector = new SocketConnector (1, ioExecutor);

      /* Change the worker timeout to make the I/O thread quit soon
       * when there's no connection to manage. */
      connector.setWorkerTimeout (0);
      
      SocketConnectorConfig connectorConfig = new SocketConnectorConfig ();
      connectorConfig.setThreadModel (ThreadModel.MANUAL);
      connectorConfig.setConnectTimeout ((int)(options.receiveTimeout / 1000));

      DefaultIoFilterChainBuilder filters = connectorConfig.getFilterChain ();
      
      filters.addLast ("codec", ClientFrameCodec.FILTER);
      
      filters.addLast 
        ("liveness", 
         new LivenessFilter (callbacks.executor (), 
                             options.livenessTimeout, 
                             options.receiveTimeout));
      
      ConnectFuture connectFuture =
        connector.connect
          (new InetSocketAddress (routerUri.host, routerUri.port),
           new IoHandler (), connectorConfig);
                        
      if (!connectFuture.join (options.receiveTimeout))
        throw new IOException ("Timed out connecting to router " + routerUri);
      
      connection = connectFuture.getSession ();

      if (routerUri.isSecure ())
        openSSL ();
      
      enableTcpNoDelay
        (connection, 
         options.connectionOptions.getBoolean ("TCP.Send-Immediately", false));
      
    } catch (RuntimeIOException ex)
    {
      // unwrap MINA's RuntimeIOException
      throw (IOException)ex.getCause ();
    } catch (UnresolvedAddressException ex)
    {
      UnknownHostException newEx = new UnknownHostException 
        ("Host name for " + routerUri + " could not be resolved");
      
      newEx.initCause (ex);
      
      throw newEx;
    }
  }
  
  /**
   * Open an TLS/SSL session for the current connection.
   */
  private void openSSL ()
    throws IOException, IllegalArgumentException
  {
    SSLFilter filter = 
      new SSLFilter (sslContextFor (options.keystore, 
                                    options.keystorePassphrase, 
                                    options.requireAuthenticatedServer));
    
    filter.setUseClientMode (true);

    connection.getFilterChain ().addFirst ("ssl", filter);
    
    /*
     * MINA would do this automatically, but we force it here so that
     * if the SSL handshake fails the exception propagates out of this
     * method.
     */
    handshakeSSL (filter);
  }

  /**
   * Start the SSL handshake process.
   */
  private void handshakeSSL (SSLFilter filter) 
    throws SSLException
  {
    filter.startSSL (connection);
    
    /*
     * Unfortunately SSL handshake is async: we have to poll.
     * exceptionCaught () has some special logic to attach SSL
     * exceptions to the session if it sees one from MINA.
     */
    long finishAt = currentTimeMillis () + options.receiveTimeout;
    
    try
    {
      while (filter.getSSLSession (connection) == null &&
             !connection.containsAttribute ("sslException") &&
             currentTimeMillis () < finishAt)
      {
        sleep (50);
      }

      SSLException sslException = 
        (SSLException)connection.getAttribute ("sslException");
    
      if (sslException != null)
        throw sslException;

    } catch (InterruptedException ex)
    {
      currentThread ().interrupt ();
    }
  }
  
  /**
   * Close the connection to the router. May be executed more than
   * once with no effect.
   * 
   * @see #addCloseListener(CloseListener)
   */
  public void close ()
  {
    close (REASON_CLIENT_SHUTDOWN, "Client shutting down normally");
  }
  
  protected void close (int reason, String message)
  {
    close (reason, message, null);
  }
  
  /** 
   * Close this connection. May be executed more than once.
   * 
   * @see #isOpen()
   */
  protected void close (int reason, String message, Throwable error)
  {
    /* Use of atomic flag allows close () to be lock-free when already
     * closed, which avoids contention when IoHandler gets a
     * sessionClosed () event triggered by this method. */
    if (!connectionOpen.getAndSet (false))
      return;
    
    synchronized (this)
    {
      if (connection != null)
      {
        // force this here, so filter cannot block callback executor shutdown
        LivenessFilter.dispose (connection);
  
        if (connection.isConnected ())
        {
          if (elvinSessionEstablished && reason == REASON_CLIENT_SHUTDOWN)
          {
            try
            {
              sendAndReceive (new DisconnRqst ());
            } catch (Exception ex)
            {
              log (DIAGNOSTIC, "Failed to cleanly disconnect", ex);
            }
          }
          
          connection.close ();
        }

        connection = null;
      }

      // any callbacks will block until this sync section ends
      fireCloseEvent (reason, message, error);
      
      ioExecutor.shutdown ();

      callbacks.shutdown ();
    }
  }
  
  private void fireCloseEvent (final int reason, final String message, 
                               final Throwable error)
  {
    if (closeListeners.hasListeners ())
    {
      callbacks.queue (new Runnable ()
      {
        public void run ()
        {
          closeListeners.fire 
            (new CloseEvent (this, reason, message, error));
        }
      });
    }
  }
  
  /**
   * Signal that this connection should be automatically closed when
   * the VM exits. This should be used with care since it creates a
   * shutdown thread on each call, and stops the connection from being
   * GC'd if it is closed manually.
   * <p>
   * This method is most suitable for small applications that want to
   * create a connection, do something with it, and then cleanly shut
   * down without having to worry about whether the VM exits normally,
   * on Ctrl+C, System.exit (), etc.
   */
  public void closeOnExit ()
  {
    Runtime.getRuntime ().addShutdownHook (new Thread ()
    {
      @Override
      public void run ()
      {
        close ();
      }
    });
  }

  /**
   * Test if this connection is open i.e. has not been
   * {@linkplain #close() closed} locally or disconnected by the
   * remote router.
   * 
   * @see #close()
   */
  public synchronized boolean isOpen ()
  {
    return connectionOpen.get () && connection.isConnected ();
  }
  
  /**
   * The router's URI.
   */
  public ElvinURI routerUri ()
  {
    return routerUri;
  }
  
  /**
   * Return the current options for the connection. These options
   * reflect any changes made after initialisation, e.g. by using
   * {@link #setReceiveTimeout(long)}, {@link
   * #setNotificationKeys(Keys)}, etc.
   */
  public ElvinOptions options ()
  {
    return options;
  }

  /**
   * The connection options established with the router. These cannot
   * be changed after connection.
   * 
   * @see #options()
   * @see #Elvin(ElvinURI, ConnectionOptions, Keys, Keys)
   */
  public ConnectionOptions connectionOptions ()
  {
    return options.connectionOptions;
  }
  
  /**
   * @see #setReceiveTimeout(long)
   * @see #options()
   */
  public long receiveTimeout ()
  {
    return options.receiveTimeout;
  }

  /**
   * Set the amount of time that must pass before the router is
   * assumed not to be responding to a request message (default is 10
   * seconds). This method can be used on a live connection, unlike
   * {@link ElvinOptions#receiveTimeout}.
   * 
   * @param receiveTimeout The new receive timeout in milliseconds.
   * 
   * @see #setLivenessTimeout(long)
   */
  public synchronized void setReceiveTimeout (long receiveTimeout)
  {
    if (receiveTimeout < 0)
      throw new IllegalArgumentException
        ("Timeout cannot be < 0: " + receiveTimeout);
    
    LivenessFilter.setReceiveTimeoutFor (connection, receiveTimeout);
    
    options.receiveTimeout = receiveTimeout;
  }
  
  /**
   * @see #setLivenessTimeout(long)
   * @see #options()
   */
  public long livenessTimeout ()
  {
    return options.livenessTimeout;
  }
  
  /**
   * Set the liveness timeout period (default is 60 seconds). If no
   * messages are seen from the router in this period a connection
   * test message is sent and if no reply is seen within the
   * {@linkplain #receiveTimeout() receive timeout period} the
   * connection is deemed to be closed. This method can be used on a
   * live connection, unlike {@link ElvinOptions#livenessTimeout}.
   * 
   * @param livenessTimeout The new liveness timeout in milliseconds.
   *          Cannot be less than 1000.
   * 
   * @see #setReceiveTimeout(long)
   */
  public synchronized void setLivenessTimeout (long livenessTimeout)
  {
    LivenessFilter.setLivenessTimeoutFor (connection, livenessTimeout);
    
    options.livenessTimeout = livenessTimeout;
  }

  /**
   * Add listener to the close event sent when the client's connection
   * to the router is disconnected. This includes normal disconnection
   * via {@link #close()}, disconnections caused by the remote router
   * shutting down, and abnormal disconnections caused by
   * communications errors.
   */
  public synchronized void addCloseListener (CloseListener listener)
  {
    closeListeners.add (listener);
  }
  
  /**
   * Remove a {@linkplain #addCloseListener(CloseListener) previously
   * added} close listener.
   */
  public synchronized void removeCloseListener (CloseListener listener)
  {
    closeListeners.remove (listener);
  }

  /**
   * Add a listener for log events emitted by the client. This
   * includes non-fatal errors, warnings and diagnostics of potential
   * problems. If no listeners are registered, log events will be
   * echoed to the standard error output.
   */
  public synchronized void addLogListener (ElvinLogListener listener)
  {
    logListeners.add (listener);
  }
  
  /**
   * Remove a {@linkplain #addLogListener(ElvinLogListener) previously
   * added} log listener.
   */
  public synchronized void removeLogListener (ElvinLogListener listener)
  {
    logListeners.remove (listener);
  }

  /**
   * Return the mutex used to synchronize access to this connection.
   * All methods on the connection that modify state or otherwise need
   * to be thread safe acquire this before operation. Clients may
   * choose to pre-acquire this mutex to execute several operations
   * atomically -- see example in
   * {@link #subscribe(String, Keys, SecureMode)}.
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
   * {@link #subscribe(String, Keys, SecureMode)} for details.
   * 
   * @param subscriptionExpr The subscription expression.
   * 
   * @return The subscription instance.
   * 
   * @throws IOException if an IO error occurs.
   * @throws InvalidSubscriptionException if the subscription expression
   *           is invalid.
   */
  public Subscription subscribe (String subscriptionExpr)
    throws IOException, InvalidSubscriptionException
  {
    return subscribe (subscriptionExpr, EMPTY_KEYS, ALLOW_INSECURE_DELIVERY);
  }
  
  /**
   * Create a new subscription with a given set of security keys to
   * enable secure delivery, but also allowing insecure notifications.
   * See {@link #subscribe(String, Keys, SecureMode)} for details.
   * 
   * @param subscriptionExpr The subscription expression.
   * @param keys The keys to add to the subscription.
   * 
   * @return The subscription instance.
   * 
   * @throws IOException if an IO error occurs.
   * @throws InvalidSubscriptionException if the subscription expression
   *           is invalid.
   */
  public Subscription subscribe (String subscriptionExpr, Keys keys)
    throws IOException, InvalidSubscriptionException
  {
    return subscribe (subscriptionExpr, keys, ALLOW_INSECURE_DELIVERY);
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
   * See {@link #subscribe(String, Keys, SecureMode)} for more details.
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
   * @throws InvalidSubscriptionException if the subscription expression
   *           is invalid.
   */
  public Subscription subscribe (String subscriptionExpr, SecureMode secureMode)
    throws IOException, InvalidSubscriptionException
  {
    return subscribe (subscriptionExpr, EMPTY_KEYS, secureMode);
  }
  
  /**
   * Create a new subscription. The returned subscription instance can
   * be used to listen for notifications, modify subscription settings
   * and unsubscribe.
   * <p>
   * 
   * See <a href="http://avis.sourceforge.net/subscription_language.html">the 
   * Elvin Subscription Language page</a> for information on how
   * subscription expressions work.
   * <p>
   *
   * There exists the possibility that, between creating a
   * subscription and adding a listener to it, the client could miss a
   * notification. If needed, the client may ensure this will not
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
   * @param keys The keys that must match notificiation keys for
   *          secure delivery.
   * @param secureMode The security mode: specifying
   *          REQUIRE_SECURE_DELIVERY means the subscription will only
   *          receive notifications that are sent by clients with keys
   *          matching the set supplied here or the global
   *          subscription key set.
   * @return The subscription instance.
   * 
   * @throws IOException if a network error occurs.
   * @throws InvalidSubscriptionException if the subscription expression
   *           is invalid.
   * 
   * @see #send(Notification, Keys, SecureMode)
   * @see #addNotificationListener(GeneralNotificationListener)
   * @see Subscription
   * @see Subscription#escapeField(String)
   * @see Subscription#escapeString(String)
   * @see SecureMode
   * @see Keys
   */
  public synchronized Subscription subscribe (String subscriptionExpr,
                                              Keys keys,
                                              SecureMode secureMode)
    throws IOException, InvalidSubscriptionException
  {
    checkConnected ();
    
    Subscription subscription =
      new Subscription (this, subscriptionExpr, secureMode, keys);
    
    subscribe (subscription);
    
    return subscription;
  }
  
  protected void subscribe (Subscription subscription)
    throws IOException
  {
    /*
     * There is a small window between sendAndReceive () and
     * subscriptions.put () where a NotifyDeliver could arrive before
     * we have registered the sub ID. Thus, we pre-register the sub
     * against (reserved) ID 0, which subscriptionFor () will use as a
     * fallback if it can't find an ID. Since these shenanigans occur
     * inside the mutex, the client will not see the halfway
     * subscribed state.
     */
 
    // pre-register subscription
    synchronized (subscriptions)
    {
      if (subscriptions.put (0L, subscription) != null)
        throw new IllegalStateException 
          ("Internal error: more than one pre-registered subscription");
    }

    try
    {
      subscription.id =
        sendAndReceive
          (new SubAddRqst (subscription.subscriptionExpr,
                           subscription.keys,
                           subscription.acceptInsecure ())).subscriptionId;
      
      // register real ID
      synchronized (subscriptions)
      {
        if (subscriptions.put (subscription.id, subscription) != null)
        {
          close (REASON_PROTOCOL_VIOLATION, 
               "Router issued duplicate subscription ID " + subscription.id);
        }
      }
    } finally
    {
      // remove pre-registration
      synchronized (subscriptions)
      {
        subscriptions.remove (0L);
      }
    }
  }

  protected void unsubscribe (Subscription subscription)
    throws IOException
  {
    sendAndReceive (new SubDelRqst (subscription.id));

    synchronized (subscriptions)
    {
      if (subscriptions.remove (subscription.id) != subscription)
        throw new IllegalStateException
          ("Internal error: invalid subscription ID " + subscription.id);
    }
  }

  protected void modifyKeys (Subscription subscription, Keys newKeys)
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

  protected void modifySubscriptionExpr (Subscription subscription,
                                         String subscriptionExpr)
    throws IOException
  {
    sendAndReceive
      (new SubModRqst (subscription.id, subscriptionExpr,
                       subscription.acceptInsecure ()));
  }
  
  protected void modifySecureMode (Subscription subscription, 
                                   SecureMode mode)
    throws IOException
  {
    sendAndReceive
      (new SubModRqst (subscription.id, "", mode == ALLOW_INSECURE_DELIVERY));
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
   * Add a listener to all notifications received by all subscriptions
   * of this connection.
   * 
   * @see #removeNotificationListener(GeneralNotificationListener)
   * @see Subscription#addListener(NotificationListener)
   */
  public void addNotificationListener (GeneralNotificationListener listener)
  {
    synchronized (this)
    {
      callbacks.flush ();
      
      notificationListeners.add (listener);
    }
  }
  
  /**
   * Remove a listener to all notifications received by all subscriptions
   * of this connection.
   * 
   * @see #addNotificationListener(GeneralNotificationListener)
   */
  public void removeNotificationListener (GeneralNotificationListener listener)
  {
    synchronized (this)
    {
      callbacks.flush ();

      notificationListeners.remove (listener);
    }
  }

  /**
   * Send a notification. See
   * {@link #send(Notification, Keys, SecureMode)} for details.
   */
  public void send (Notification notification)
    throws IOException
  {
    send (notification, EMPTY_KEYS, ALLOW_INSECURE_DELIVERY);
  }
  
  /**
   * Send a notification with a set of keys but <b>with no requirement
   * for secure delivery</b>: use <code>send (notification,
   * REQUIRE_SECURE_DELIVERY, keys)</code> if you want only
   * subscriptions with matching keys to receive a notification.
   * <p>
   * See {@link #send(Notification, Keys, SecureMode)} for more details.
   * 
   * @param notification The notification to send.
   * @param keys The keys to attach to the notification.
   * 
   * @throws IOException if an IO error occurs during sending.
   */
  public void send (Notification notification, Keys keys)
    throws IOException
  {
    send (notification, keys, ALLOW_INSECURE_DELIVERY);
  }
  
  /**
   * Send a notification with a specified security mode. Be careful
   * when using REQUIRE_SECURE_DELIVERY with this method: if you
   * haven't specified any global notification keys via
   * {@link #setKeys(Keys, Keys)} or
   * {@link #setNotificationKeys(Keys)}, the notification will never
   * be able to to be delivered.
   * <p>
   * See {@link #send(Notification, Keys, SecureMode)} for more details.
   * 
   * @param notification The notification to send.
   * @param secureMode The security requirement.
   * 
   * @throws IOException if an IO error occurs during sending.
   */
  public void send (Notification notification, SecureMode secureMode)
    throws IOException
  {
    send (notification, EMPTY_KEYS, secureMode);
  }

  /**
   * Send a notification.
   * 
   * @param notification The notification to send.
   * @param keys The keys that must match for secure delivery.
   * @param secureMode The security requirement.
   *          REQUIRE_SECURE_DELIVERY means the notification can only
   *          be received by subscriptions with keys matching the set
   *          supplied here (or the connections'
   *          {@linkplain #setNotificationKeys(Keys) global notification keys}).
   * @throws IOException if an IO error occurs.
   * 
   * @see #subscribe(String, Keys, SecureMode)
   * @see Notification
   * @see SecureMode
   * @see Keys
   */
  public synchronized void send (Notification notification,
                                 Keys keys,
                                 SecureMode secureMode)
    throws IOException
  {
    checkNotNull (secureMode, "Secure mode");
    checkNotNull (keys, "Keys");
    
    send (new NotifyEmit (notification.attributes,
                          secureMode == ALLOW_INSECURE_DELIVERY, keys));
  }
  
  /**
   * @see #setNotificationKeys(Keys)
   */
  public Keys notificationKeys ()
  {
    return options.notificationKeys;
  }
  
  /**
   * Set the connection-wide notification keys used to secure delivery
   * of notifications.
   * 
   * @param newNotificationKeys The new notification keys. These
   *          automatically apply to all notifications, exactly as if
   *          they were added to the keys in the
   *          {@linkplain #send(Notification, Keys, SecureMode) send}
   *          call.
   * 
   * @throws IOException if an IO error occurs.
   * 
   * @see #setSubscriptionKeys(Keys)
   * @see #setKeys(Keys, Keys)
   */
  public void setNotificationKeys (Keys newNotificationKeys)
    throws IOException
  {
    setKeys (newNotificationKeys, options.subscriptionKeys);
  }
  
  /**
   * @see #setSubscriptionKeys(Keys)
   */
  public Keys subscriptionKeys ()
  {
    return options.notificationKeys;
  }
  
  /**
   * Set the connection-wide subscription keys used to secure receipt
   * of notifications.
   * 
   * @param newSubscriptionKeys The new subscription keys. These
   *          automatically apply to all subscriptions, exactly as if
   *          they were added to the keys in the
   *          {@linkplain #subscribe(String, Keys, SecureMode) subscription},
   *          call. This includes currently existing subscriptions.
   * 
   * @throws IOException if an IO error occurs.
   * 
   * @see #setNotificationKeys(Keys)
   * @see #setKeys(Keys, Keys)
   */
  public void setSubscriptionKeys (Keys newSubscriptionKeys)
    throws IOException
  {
    setKeys (options.notificationKeys, newSubscriptionKeys);
  }
  
  /**
   * Change the connection-wide keys used to secure the receipt and
   * delivery of notifications.
   * 
   * @param newNotificationKeys The new notification keys. These
   *          automatically apply to all notifications, exactly as if
   *          they were added to the keys in the
   *          {@linkplain #send(Notification, Keys, SecureMode) send}
   *          call.
   * @param newSubscriptionKeys The new subscription keys. These
   *          automatically apply to all subscriptions, exactly as if
   *          they were added to the keys in the
   *          {@linkplain #subscribe(String, Keys, SecureMode) subscription},
   *          call. This applies to all existing and future
   *          subscriptions.
   * 
   * @throws IOException if an IO error occurs.
   */
  public void setKeys (Keys newNotificationKeys, Keys newSubscriptionKeys)
    throws IOException
  {
    checkNotNull (newNotificationKeys, "Notification keys");
    checkNotNull (newSubscriptionKeys, "Subscription keys");
    
    synchronized (this)
    {
      Keys.Delta deltaNotificationKeys =
        options.notificationKeys.deltaFrom (newNotificationKeys);
      Keys.Delta deltaSubscriptionKeys =
        options.subscriptionKeys.deltaFrom (newSubscriptionKeys);
  
      if (!deltaNotificationKeys.isEmpty () || 
          !deltaSubscriptionKeys.isEmpty ())
      {
        sendAndReceive
          (new SecRqst
             (deltaNotificationKeys.added, deltaNotificationKeys.removed,
              deltaSubscriptionKeys.added, deltaSubscriptionKeys.removed));
        
        options.notificationKeys = newNotificationKeys;
        options.subscriptionKeys = newSubscriptionKeys;

        callbacks.flush ();
      }
    }
  }
  
  private void send (Message message)
    throws IOException
  {
    checkConnected ();
  
    connection.write (message);
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
    throws IOException, RuntimeInterruptedException
  {
    send (request);
   
    return receiveReply (request);
  }
  
  /**
   * Block the calling thread for up to receiveTimeout millis waiting
   * for a reply message to arrive from the router.
   */
  private XidMessage receiveReply ()
    throws IOException, RuntimeInterruptedException
  {
    synchronized (replyLock)
    {
      if (lastReply == null)
      {
        try
        {
          replyLock.wait (options.receiveTimeout);
        } catch (InterruptedException ex)
        {
          // clear reply and continue interrupt
          lastReply = null;
          
          currentThread ().interrupt ();
          
          throw new RuntimeInterruptedException (ex);
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
        
        throw new IOException 
          ("Timeout error: router did not respond within " + 
           (options.receiveTimeout / 1000) + " seconds");
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
   * @throws IOException if no suitable reply is seen or a network
   *           error occurs.
   */
  @SuppressWarnings("unchecked")
  private <R extends XidMessage> R receiveReply (RequestMessage<R> request)
    throws IOException
  {
    XidMessage reply = receiveReply ();
    
    if (reply.xid != request.xid)
    {
      String message = 
        "Transaction ID mismatch in reply from router: " +
         "reply " + className (reply) + " XID " + reply.xid + 
         " != request " + className (request) + " XID " + request.xid;
      
      close (REASON_PROTOCOL_VIOLATION, message);
      
      throw new IOException ("Protocol violation: " + message);      
    } else if (request.replyType ().isAssignableFrom (reply.getClass ()))
    {
      return (R)reply;
    } else if (reply instanceof Nack)
    {
      Nack nack = (Nack)reply;
      
      // 21xx NACK code => subscription error
      if (nack.error / 100 == 21)
        throw new InvalidSubscriptionException (request, nack);
      else
        throw new RouterNackException (request, nack);
    } else
    {
      String message = "Received a " + className (reply) + 
                       ": was expecting " + className (request.replyType ());
      
      // this is a serious error: it's unlikely we can continue the session
      close (REASON_PROTOCOL_VIOLATION, message);
      
      throw new IOException ("Protocol violation: " + message);
    }
  }
  
  /**
   * Handle replies to client-initiated messages by delivering them
   * back to the waiting thread.
   */
  protected void deliverReply (XidMessage reply)
  {
    synchronized (replyLock)
    {
      if (lastReply != null)
      {
        /*
         * Closing down in JUTestClient.multiThread () gets multiple
         * SubReply's after connection shutdown. For now, we just
         * ignore them when the connection is not open.
         */           
        if (connectionOpen.get ())
        {
          throw new IllegalStateException 
            ("Reply buffer overflow: " + reply.name () + 
             " arrived with a " + lastReply.name () + " not collected");
        }
      }
      
      lastReply = reply;
      
      replyLock.notify ();
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
      case DropWarn.ID:
        log (WARNING, "Router sent a dropped packet warning: " +
                      "a message may have been discarded due to congestion");
        break;
      case ErrorMessage.ID:
        handleErrorMessage ((ErrorMessage)message);
        break;
      case LivenessFailureMessage.ID:
        close (REASON_ROUTER_STOPPED_RESPONDING, "Router stopped responding");
        break;
      default:
        close (REASON_PROTOCOL_VIOLATION, 
               "Received unexpected message type " + message.name ());
    }
  }

  private void handleNotifyDeliver (NotifyDeliver message)
  {
    /*
     * Notify callbacks are executed from the callback thread, since
     * firing an event in this thread causes deadlock if a listener
     * triggers a receive (), at which point the IO processor thread
     * will be waiting for a reply that cannot be processed since it
     * is busy calling this method.
     */
    try
    {
      callbacks.queue
        (new NotifyCallback (message, 
                             subscriptionsFor (message.secureMatches), 
                             subscriptionsFor (message.insecureMatches)));
    } catch (IllegalStateException ex)
    {
      close (REASON_PROTOCOL_VIOLATION, ex.getMessage ());
    }
  }
  
  private void handleDisconnect (Disconn disconn)
  {
    int reason;
    String message;
    
    switch (disconn.reason)
    {
      case Disconn.REASON_SHUTDOWN:
        reason = REASON_ROUTER_SHUTDOWN;
        message =
          disconn.hasArgs () ? disconn.args : "Router is shutting down";
        break;
      case Disconn.REASON_SHUTDOWN_REDIRECT:
        // todo handle REASON_SHUTDOWN_REDIRECT properly
        reason = REASON_ROUTER_SHUTDOWN;
        message = "Router suggested redirect to " + disconn.args;
        break;
      case Disconn.REASON_PROTOCOL_VIOLATION:
        reason = REASON_PROTOCOL_VIOLATION;
        message =
          disconn.hasArgs () ? disconn.args : "Protocol violation";
        break;
      default:
        reason = REASON_PROTOCOL_VIOLATION;
        message = "Protocol violation";
    }
    
    close (reason, message);
  }
  
  private void handleErrorMessage (ErrorMessage message)
  {
    close (REASON_PROTOCOL_VIOLATION, 
           "Protocol error in communication with router: " + 
           message.formattedMessage (),
           message.error);
  }
  
  /**
   * Generate a subscription set for a given set of ID's
   *
   * @throws IllegalStateException if any of the ID's cannot be
   * resolved to a subscription.
   */
  private Set<Subscription> subscriptionsFor (long [] ids)
    throws IllegalStateException
  {
    if (ids.length == 0)
      return emptySet ();
    
    HashSet<Subscription> subscriptionSet = new HashSet<Subscription> ();
 
    synchronized (subscriptions)
    {
      for (long id : ids)
      {
        Subscription subscription = subscriptions.get (id);
      
        if (subscription == null)
          subscription = subscriptions.get (0L);
      
        if (subscription != null)
          subscriptionSet.add (subscription);
        else
          throw new IllegalStateException 
            ("Received notification for invalid subscription ID " + id);
      }
    }

    return subscriptionSet;
  }
  
  protected void checkConnected ()
    throws IOException
  {
    IoSession session = connection;
    
    if (session == null)
      throw new NotConnectedException ("Connection is closed");
    else if (!session.isConnected ())
      throw new NotConnectedException ("Not connected to router");
  }
  
  protected void log (ElvinLogEvent.Type type, String message)
  {
    log (type, message, null);
  }
  
  protected void log (ElvinLogEvent.Type type, String message, Throwable error)
  {
    if (logListeners.hasListeners ())
    {
      logListeners.fire (new ElvinLogEvent (this, type, message, error));
    } else
    {
      System.err.println 
        (getClass ().getName () + ": " + type + ": " + message);
      
      if (error != null)
        error.printStackTrace ();
    }
  }
  
  private class NotifyCallback implements Runnable
  {
    private Notification notification;
    private Set<Subscription> secureMatches;
    private Set<Subscription> insecureMatches;

    public NotifyCallback (NotifyDeliver message,
                           Set<Subscription> secureMatches,
                           Set<Subscription> insecureMatches)
    {
      this.notification = new Notification (message);
      this.secureMatches = secureMatches;
      this.insecureMatches = insecureMatches;
    }

    public void run ()
    {
      if (notificationListeners.hasListeners ())
      {
        notificationListeners.fire
          (new GeneralNotificationEvent (Elvin.this, notification,
                                         insecureMatches,
                                         secureMatches));
      }     
        
      Map<String, Object> data = new HashMap<String, Object> ();

      fireSubscriptionNotify (secureMatches, true, notification, data);
      fireSubscriptionNotify (insecureMatches, false, notification, data);
    }
    
    /**
     * Fire notification events for subscription listeners.
     * 
     * @param matches The subscription ID's
     * @param secure Whether the notification was secure.
     * @param ntfn The notification.
     * @param data General data attached to the event for the client's use.
     */
    private void fireSubscriptionNotify (Set<Subscription> matches,
                                         boolean secure,
                                         Notification ntfn,
                                         Map<String, Object> data)
    {
      for (Subscription subscription : matches)
      {
        if (subscription.hasListeners ())
        {
          subscription.notifyListeners
            (new NotificationEvent (subscription, ntfn, secure, data));
        }
      }
    }
  }

  private class IoHandler extends IoHandlerAdapter
  {
    public IoHandler ()
    {
      // zip
    }
    
    /**
     * Handle exceptions from MINA.
     */
    @Override
    public void exceptionCaught (IoSession session, Throwable cause)
      throws Exception
    {
      if (cause instanceof SSLException)
      {
        // tunnel SSL exception to handshakeSSL ()
        if (!elvinSessionEstablished && connection != null)
          connection.setAttribute ("sslException", cause);
        else
          close (REASON_IO_ERROR, "SSL error", cause);
      } else if (cause instanceof InterruptedException)
      {
        log (DIAGNOSTIC, "I/O thread interrupted");
      } else if (cause instanceof IOException)
      {
        close (REASON_IO_ERROR, "I/O error communicating with router", cause);
      } else
      {
        log (ERROR, "Unexpected exception in Elvin client", cause);
      }
    }

    /**
     * Handle incoming messages from MINA.
     */
    @Override
    public void messageReceived (IoSession session, Object message)
      throws Exception
    {
      try
      {
        if (message instanceof XidMessage)
          deliverReply ((XidMessage)message);
        else if (connectionOpen.get ())
          handleMessage ((Message)message);
      } catch (RuntimeInterruptedException ex)
      {
        currentThread ().interrupt ();
        
        throw (InterruptedException)ex.getCause ();
      }
    }
    
    @Override
    public void sessionClosed (IoSession session)
      throws Exception
    {
      // NB: close () does nothing if connection already closed normally
      close (REASON_ROUTER_SHUTDOWN_UNEXPECTEDLY,
             "Connection to router closed without warning");
    }
  }
}
