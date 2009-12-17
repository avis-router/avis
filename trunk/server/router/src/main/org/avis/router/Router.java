package org.avis.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.Closeable;
import java.io.IOException;

import java.net.InetAddress;
import java.net.URI;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.util.ExceptionMonitor;

import org.avis.common.ElvinURI;
import org.avis.config.Options;
import org.avis.io.ClientFrameCodec;
import org.avis.io.ExceptionMonitorLogger;
import org.avis.io.FrameTooLargeException;
import org.avis.io.messages.ConfConn;
import org.avis.io.messages.ConnRply;
import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.DisconnRply;
import org.avis.io.messages.DisconnRqst;
import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.Notify;
import org.avis.io.messages.NotifyDeliver;
import org.avis.io.messages.NotifyEmit;
import org.avis.io.messages.QuenchPlaceHolder;
import org.avis.io.messages.SecRply;
import org.avis.io.messages.SecRqst;
import org.avis.io.messages.SubAddRqst;
import org.avis.io.messages.SubDelRqst;
import org.avis.io.messages.SubModRqst;
import org.avis.io.messages.SubRply;
import org.avis.io.messages.TestConn;
import org.avis.io.messages.UNotify;
import org.avis.io.messages.XidMessage;
import org.avis.security.Keys;
import org.avis.subscription.parser.ConstantExpressionException;
import org.avis.subscription.parser.ParseException;
import org.avis.util.ConcurrentHashSet;
import org.avis.util.Filter;
import org.avis.util.IllegalConfigOptionException;
import org.avis.util.ListenerList;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

import static org.apache.mina.core.future.IoFutureListener.CLOSE;
import static org.apache.mina.core.session.IdleStatus.READER_IDLE;

import static org.avis.common.Common.CLIENT_VERSION_MAJOR;
import static org.avis.common.Common.CLIENT_VERSION_MINOR;
import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.io.FrameCodec.setMaxFrameLengthFor;
import static org.avis.io.LegacyConnectionOptions.setWithLegacy;
import static org.avis.io.Net.enableTcpNoDelay;
import static org.avis.io.Net.hostIdFor;
import static org.avis.io.Net.idFor;
import static org.avis.io.Net.remoteHostAddressFor;
import static org.avis.io.messages.Disconn.REASON_PROTOCOL_VIOLATION;
import static org.avis.io.messages.Disconn.REASON_SHUTDOWN;
import static org.avis.io.messages.Nack.EMPTY_ARGS;
import static org.avis.io.messages.Nack.EXP_IS_TRIVIAL;
import static org.avis.io.messages.Nack.IMPL_LIMIT;
import static org.avis.io.messages.Nack.NOT_IMPL;
import static org.avis.io.messages.Nack.NO_SUCH_SUB;
import static org.avis.io.messages.Nack.PARSE_ERROR;
import static org.avis.io.messages.Nack.PROT_INCOMPAT;
import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.trace;
import static org.avis.logging.Log.warn;
import static org.avis.router.ConnectionOptionSet.CONNECTION_OPTION_SET;
import static org.avis.router.RouterOptionSet.setSendQueueDropPolicy;
import static org.avis.router.RouterOptionSet.setSendQueueMaxLength;
import static org.avis.security.DualKeyScheme.Subset.CONSUMER;
import static org.avis.security.DualKeyScheme.Subset.PRODUCER;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.subscription.parser.SubscriptionParserBase.expectedTokensFor;
import static org.avis.util.Text.className;
import static org.avis.util.Text.formatNotification;
import static org.avis.util.Text.shortException;

public class Router implements IoHandler, Closeable
{
  static
  {
    // route MINA exceptions to log
    ExceptionMonitor.setInstance (ExceptionMonitorLogger.INSTANCE);
  }
  
  private static final String ROUTER_VERSION =
    System.getProperty ("avis.router.version", "<unknown>");
  
  private IoManager ioManager;

  private RouterOptions routerOptions;
  private ConcurrentHashSet<IoSession> sessions;

  private volatile boolean closing;
  
  private ListenerList<NotifyListener> notifyListeners;
  private ListenerList<CloseListener> closeListeners;

  public int sentNotificationCount;
  public int receivedNotificationCount;

  /**
   * Create an instance with default configuration.
   */
  public Router ()
    throws IOException
  {
    this (DEFAULT_PORT);
  }
  
  /**
   * Shortcut to create an instance listening to localhost:port.
   */
  public Router (int port)
    throws IOException
  {
    this (new RouterOptions (port));
  }
  
  /**
   * Create a new instance.
   * 
   * @param options The router configuration options. Note that, due
   *                to a MINA limitation, the IO.Use-Direct-Buffers
   *                option applies globally, so using multiple router
   *                instances with this option set to different values
   *                will clash.
   * 
   * @throws IOException if an network error during router
   *                 initialisation.
   * @throws IllegalConfigOptionException If an option in the configuration
   *                 options is invalid.
   */
  @SuppressWarnings ("unchecked")
  public Router (RouterOptions options)
    throws IOException, IllegalConfigOptionException
  {
    this.sentNotificationCount = 0;
    this.receivedNotificationCount = 0;

    this.notifyListeners = 
      new ListenerList<NotifyListener>
        (NotifyListener.class, "notifyReceived", Notify.class, Keys.class);
    this.closeListeners = 
      new ListenerList<CloseListener>
        (CloseListener.class, "routerClosing", Router.class);
    
    this.routerOptions = options;
    this.sessions = new ConcurrentHashSet<IoSession> ();
    
    URI keystoreUri = (URI)options.get ("TLS.Keystore");
    
    if (keystoreUri.toString ().length () == 0)
      keystoreUri = null;
    else
      keystoreUri = routerOptions.toAbsoluteURI (keystoreUri);
    
    this.ioManager = 
      new IoManager 
        (keystoreUri, options.getString ("TLS.Keystore-Passphrase"),
         options.getInt ("Packet.Max-Length"),
         options.getInt ("IO.Low-Memory-Protection.Min-Free-Memory"),
         options.getBoolean ("IO.Use-Direct-Buffers")); 
    
    /*
     * Setup IO filter chain. NOTE re thread pool: we do this in order
     * to install the IO event queue throttle to handle spammy
     * clients. It's not clear that we gain any other benefit from it
     * since Avis notification processing is non-blocking. See
     * http://mina.apache.org/configuring-thread-model.html.
     */
    DefaultIoFilterChainBuilder filters = new DefaultIoFilterChainBuilder ();

    filters.addLast ("codec", ClientFrameCodec.FILTER);
    
    /*
     * We use a thread pool to avoid bunging up the IO processors when
     * under load. 
     */
    filters.addLast ("threadPool", ioManager.createThreadPoolFilter ());

    boolean bindSucceeded = false;
    
    try
    {
      ioManager.bind 
        (options.listenURIs (), this, filters, 
         (Filter<InetAddress>)routerOptions.get ("Require-Authenticated"));
      
      bindSucceeded = true;
    } finally
    {
      if (!bindSucceeded)
        ioManager.close ();
    }
  }

  public IoManager ioManager ()
  {
    return ioManager;
  }
  
  /**
   * The set of MINA I/O acceptors accepting client connections.
   */
  public Collection<IoAcceptor> ioAcceptors ()
  {
    return ioManager.acceptorsFor (routerOptions.listenURIs ());
  }
  
  /**
   * Close all connections synchronously. Close listeners are notified
   * before shutdown commences. May be called more than once with no
   * effect.
   */
  public void close ()
  {
    synchronized (this)
    {
      if (closing)
        return;
      
      closing = true; 
    }
    
    closeListeners.fire (this);
    
    // stop accepting new connections    
    ioManager.unbind (routerOptions.listenURIs ());

    // shut down existing connections
    Disconn disconnMessage = 
      new Disconn (REASON_SHUTDOWN, "Router is shutting down");
    
    for (IoSession session : sessions)
    {
      Connection connection = peekConnectionFor (session);
     
      session.suspendRead ();
      
      if (connection != null)
      {
        connection.lockWrite ();

        try
        {
          if (connection.isOpen ())
          {
            send (session, disconnMessage).addListener (CLOSE);

            connection.close ();
          } else
          {
            session.close (false);
          }
        } finally
        {
          connection.unlockWrite ();
        }
      } else
      {
        session.close (false);
      }
    }
    
    waitForAllSessionsToClose ();
    
    ioManager.close ();
  }

  private void waitForAllSessionsToClose ()
  {
    long finish = currentTimeMillis () + 10000;
    
    try
    {
      while (!sessions.isEmpty () && currentTimeMillis () < finish)
        sleep (100);
    } catch (InterruptedException ex)
    {
      currentThread ().interrupt ();
    }
    
    if (!sessions.isEmpty ())
    {
      warn ("Sessions took too long to close: " +
            sessions.size () + " still open", this);
    }
    
    sessions.clear ();
  }

  public Set<ElvinURI> listenURIs ()
  {
    return routerOptions.listenURIs ();
  }
  
  public Options options ()
  {
    return routerOptions;
  }
  
  /**
   * Return a static collection of open connections at the time of calling.
   */
  public List<Connection> connections ()
  {
    ArrayList<Connection> connections = 
      new ArrayList<Connection> (sessions.size ());
    
    for (IoSession session : sessions)
    {
      Connection connection = peekConnectionFor (session);
      
      if (connection == null)
        continue;
      
      connections.add (connection);
    }
    
    return connections;
  }

  /**
   * Used for testing to simulate server hanging: server stops
   * responding to messages but keeps connection open.
   * 
   * @see #testSimulateUnhang()
   */
  public void testSimulateHang ()
  {
    // cause messageReceived () to stop processing
    closing = true;
  }
  
  /**
   * Undo the effect of {@link #testSimulateHang()}.
   */
  public void testSimulateUnhang ()
  {
    closing = false;
  }
  
  /**
   * Add a listener that will be invoked when the router is about to
   * close down.
   * 
   * @see #removeCloseListener(CloseListener)
   */
  public void addCloseListener (CloseListener listener)
  {
    synchronized (closeListeners)
    {      
      closeListeners.add (listener);
    }
  }
  
  /**
   * Undo the effect of {@link #addCloseListener(CloseListener)}.
   */
  public void removeCloseListener (CloseListener listener)
  {
    synchronized (closeListeners)
    {
      closeListeners.remove (listener);
    }
  }
  
  /**
   * Get a list of the current close event listeners.
   */
  public List<CloseListener> closeListeners ()
  {
    return closeListeners.asList ();
  }
  
  /**
   * Add a listener that will be invoked whenever a Notify message is
   * handled for delivery.
   * 
   * @see #removeNotifyListener(NotifyListener)
   * @see #injectNotify(Notify)
   */
  public void addNotifyListener (NotifyListener listener)
  {
    synchronized (notifyListeners)
    {
      notifyListeners.add (listener);
    }
  }
  
  /**
   * Undo the effect of {@link #addNotifyListener(NotifyListener)}.
   */
  public void removeNotifyListener (NotifyListener listener)
  {
    synchronized (notifyListeners)
    {
      notifyListeners.remove (listener);
    }
  }
  
  // IoHandler interface

  public void messageReceived (IoSession session, Object messageObject)
    throws Exception
  {
    if (closing || connectionClosed (session))
      return;
    
    if (shouldLog (TRACE))
    {
      trace ("Server got message from " + idFor (session) +
             ": " + messageObject, this);
    }
    
    Message message = (Message)messageObject;

    try
    {
      switch (message.typeId ())
      {
        case ConnRqst.ID:
          handleConnRqst (session, (ConnRqst)message);
          break;
        case DisconnRqst.ID:
          handleDisconnRqst (session, (DisconnRqst)message);
          break;
        case SubAddRqst.ID:
          handleSubAddRqst (session, (SubAddRqst)message);
          break;
        case SubModRqst.ID:
          handleSubModRqst (session, (SubModRqst)message);
          break;
        case SubDelRqst.ID:
          handleSubDelRqst (session, (SubDelRqst)message);
          break;
        case NotifyEmit.ID:
          handleNotifyEmit (session, (NotifyEmit)message);
          break;
        case SecRqst.ID:
          handleSecRqst (session, (SecRqst)message);
          break;
        case TestConn.ID:
          handleTestConn (session);
          break;
        case UNotify.ID:
          handleUnotify ((UNotify)message);
          break;
        case ErrorMessage.ID:
          handleError (session, (ErrorMessage)message);
          break;
        case QuenchPlaceHolder.ID:
          handleQuench (session, (QuenchPlaceHolder)message);
          break;
        default:
          warn
            ("Server got an unhandleable message type: " + message, this);
      }
    } catch (ProtocolCodecException ex)
    {
      /*
       * A message processing method detected a protocol violation
       * e.g. attempt to remove non existent subscription.
       */
      disconnectProtocolViolation (session, message, ex.getMessage (), ex);
    }
  }

  private void handleConnRqst (IoSession session, ConnRqst message)
    throws ProtocolCodecException
  {
    if (peekConnectionFor (session) != null)
      throw new ProtocolCodecException ("Already connected");
    
    Connection connection =
      new Connection (session, routerOptions, message.options,
                      message.subscriptionKeys, message.notificationKeys);
    
    int maxKeys = connection.options.getInt ("Connection.Max-Keys");
    
    if (message.versionMajor != CLIENT_VERSION_MAJOR ||
        message.versionMinor > CLIENT_VERSION_MINOR)
    {
      send (session,
            new Nack (message, PROT_INCOMPAT,
                      "Max supported protocol version is " +
                       + CLIENT_VERSION_MAJOR + '.' + CLIENT_VERSION_MINOR +
                       ": use a connection URI like " +
                       "elvin:" + CLIENT_VERSION_MAJOR + '.' + 
                       CLIENT_VERSION_MINOR + "//hostname to specify " +
                       "protocol version"));
    } else if (message.notificationKeys.size () > maxKeys ||
               message.subscriptionKeys.size () > maxKeys)
    {
      nackLimit (session, message, "Too many keys");
    } else
    {
      if (!enableTcpNoDelay 
            (session, connection.options.getInt ("TCP.Send-Immediately") != 0))
      {
        connection.options.remove ("TCP.Send-Immediately"); 
      }

      // we don't support per-session Receive-Queue.Max-Length
      connection.options.remove ("Receive-Queue.Max-Length");
      
      Map<String, Object> options = connection.options.accepted ();

      // add router ID
      setWithLegacy (options, 
                     "Vendor-Identification", "Avis " + ROUTER_VERSION);
      
      connection.lockWrite ();
      
      try
      {
        setConnection (session, connection);
        
        send (session, new ConnRply (message, options));
      } finally
      {
        connection.unlockWrite ();
      }
    }
  }

  /**
   * NOTE: the spec says it's a violation to add the same key more
   * than once or to remove a non-existent key (sec 7.4.8) and
   * should be reported as a protocol violation. Avis currently
   * doesn't enforce this since neither of these cases has any
   * effect on its key collections and the check would add overhead.
   */
  private void handleSecRqst (IoSession session, SecRqst message)
    throws NoConnectionException
  {
    Connection connection = writeableConnectionFor (session);
    
    try
    {
      message.addNtfnKeys.hashPrivateKeysForRole (PRODUCER);
      message.delNtfnKeys.hashPrivateKeysForRole (PRODUCER);
      
      message.addSubKeys.hashPrivateKeysForRole (CONSUMER);
      message.delSubKeys.hashPrivateKeysForRole (CONSUMER);
      
      Keys newNtfnKeys = connection.notificationKeys.delta
        (message.addNtfnKeys, message.delNtfnKeys);
  
      Keys newSubKeys = connection.subscriptionKeys.delta
        (message.addSubKeys, message.delSubKeys);

      if (connection.connectionKeysFull (newNtfnKeys, newSubKeys))
      {
        nackLimit (session, message, "Too many keys");
      } else
      {
        connection.notificationKeys = newNtfnKeys;
        connection.subscriptionKeys = newSubKeys;
      
        send (session, new SecRply (message));
      }
    } finally
    {
      connection.unlockWrite ();
    }
  }

  private void handleDisconnRqst (IoSession session, DisconnRqst message)
    throws NoConnectionException
  {
    Connection connection = writeableConnectionFor (session);

    try
    {
      connection.close ();
      
      send (session, new DisconnRply (message)).addListener (CLOSE);
    } finally
    {
      connection.unlockWrite ();
    }
  }
  
  private void handleSubAddRqst (IoSession session, SubAddRqst message)
    throws NoConnectionException
  {
    Connection connection = writeableConnectionFor (session);

    try
    {
      if (connection.subscriptionsFull ())
      {
        nackLimit (session, message, "Too many subscriptions");
      } else if (connection.subscriptionTooLong (message.subscriptionExpr))
      {
        nackLimit (session, message, "Subscription too long");
      } else if (connection.subscriptionKeysFull (message.keys))
      {
        nackLimit (session, message, "Too many keys");
      } else
      {
        Subscription subscription =
          new Subscription (message.subscriptionExpr,
                            message.keys, message.acceptInsecure);
       
        connection.addSubscription (subscription);
  
        send (session, new SubRply (message, subscription.id));
      }
    } catch (ParseException ex)
    {
      nackParseError (session, message, message.subscriptionExpr, ex);
    } finally
    {
      connection.unlockWrite ();
    }
  }

  private void handleSubModRqst (IoSession session, SubModRqst message)
    throws NoConnectionException  
  {
    Connection connection = writeableConnectionFor (session);

    try
    {
      Subscription subscription =
        connection.subscriptionFor (message.subscriptionId);
      
      message.addKeys.hashPrivateKeysForRole (CONSUMER);
      message.delKeys.hashPrivateKeysForRole (CONSUMER);
      
      Keys newKeys = 
        subscription.keys.delta (message.addKeys, message.delKeys);

      if (connection.subscriptionKeysFull (newKeys))
      {
        nackLimit (session, message, "Too many keys");
      } else if (connection.subscriptionTooLong (message.subscriptionExpr))
      {
        nackLimit (session, message, "Subscription too long");
      } else
      {
        if (message.subscriptionExpr.length () > 0)
          subscription.updateExpression (message.subscriptionExpr);
  
        subscription.keys = newKeys;
        subscription.acceptInsecure = message.acceptInsecure;
        
        send (session, new SubRply (message, subscription.id));
      }
    } catch (ParseException ex)
    {
      nackParseError (session, message, message.subscriptionExpr, ex);
    } catch (InvalidSubscriptionException ex)
    {
      nackNoSub (session, message, message.subscriptionId, ex.getMessage ());
    } finally
    {
      connection.unlockWrite ();
    }
  }

  private void handleSubDelRqst (IoSession session, SubDelRqst message)
    throws NoConnectionException
  {
    Connection connection = writeableConnectionFor (session);
    
    try
    {
      if (connection.removeSubscription (message.subscriptionId) != null)
        send (session, new SubRply (message, message.subscriptionId));
      else
        nackNoSub (session, message, message.subscriptionId,
                   "Invalid subscription ID");
    } finally
    {
      connection.unlockWrite ();
    }
  }
  
  private void handleNotifyEmit (IoSession session, NotifyEmit message)
    throws NoConnectionException
  {
    if (shouldLog (TRACE))
      logNotification (session, message);
    
    message.keys.hashPrivateKeysForRole (PRODUCER);
    
    Connection connection = connectionFor (session);
    
    connection.receivedNotificationCount++;
    
    deliverNotification (message, connection.notificationKeys);
  }

  private void handleUnotify (UNotify message)
  {
    if (shouldLog (TRACE))
      logNotification (null, message);
    
    message.keys.hashPrivateKeysForRole (PRODUCER);
    
    deliverNotification (message, EMPTY_KEYS);
  }
  
  /**
   * Inject a notification from an outside producer.
   */
  public void injectNotify (Notify message)
  {
    deliverNotification (message, EMPTY_KEYS);
  }

  /**
   * Deliver a notification message to subscribers.
   * 
   * @param message The message (a UNotify or a NotifyEmit).
   * @param notificationKeys The global notification keys that apply
   *          to the message. These are in addition to any keys
   *          attached to the message itself.
   */
  private void deliverNotification (Notify message, Keys notificationKeys)
  {
    receivedNotificationCount++;
    
    for (IoSession session : sessions)
    {
      Connection connection = peekConnectionFor (session);
      
      if (connection == null)
        continue;
      
      connection.lockRead ();

      try
      {        
        if (!connection.isOpen ())
          continue;
        
        SubscriptionMatch matches =
          connection.matchSubscriptions (message.attributes,
                                         notificationKeys,
                                         message.keys,
                                         message.deliverInsecure);

        if (matches.matched ())
        {
          if (shouldLog (TRACE))
          {
            trace ("Delivering notification " + idFor (message) + 
                   " to client " + idFor (session), this);
          }
          
          send (session, new NotifyDeliver (message,
                                            matches.secure (),
                                            matches.insecure ()));
          
          // update stats
          sentNotificationCount++;
          connection.sentNotificationCount++;
          
          incrementNotificationCount (matches.secure);
          incrementNotificationCount (matches.insecure);
        }
      } catch (RuntimeException ex)
      {
        /*
         * Do not allow "normal" runtime exceptions to abort delivery
         * to other clients. Log and continue to next client.
         */
        alarm ("Exception while delivering notification", this, ex);
      } finally
      {
        connection.unlockRead ();
      }
    }
    
    if (notifyListeners.hasListeners ())
      notifyListeners.fire (message, notificationKeys);
  }

  private static void handleTestConn (IoSession session)
  {
    // if no other outgoing messages are waiting, send a confirm message
    if (session.getScheduledWriteMessages () == 0)
      send (session, ConfConn.INSTANCE);
  }
  
  private static void handleQuench (IoSession session,
                                    QuenchPlaceHolder message)
  {
    diagnostic
      ("Rejecting quench request from client " + idFor (session) + 
       ": quench is not supported", Router.class);
    
    send (session, new Nack (message, NOT_IMPL, "Quench not supported"));
  }

  private static void handleError (IoSession session, 
                                   ErrorMessage errorMessage)
  {
    String message;
    
    if (errorMessage.error instanceof FrameTooLargeException)
    {
      // add helpful advisory for client that exceeds max frame size
      message = 
        errorMessage.error.getMessage () + 
        ". Use the Packet.Max-Length connection option to increase the " +
        "maximum notification size.";
    } else
    {
      message = errorMessage.error.getMessage ();
      
      if (message == null)
        message = "Exception: " + className (errorMessage.error);
    }
    
    disconnectProtocolViolation (session, errorMessage.cause, message, null);
  }

  /**
   * Handle a protocol violation by a client disconnecting with the
   * REASON_PROTOCOL_VIOLATION code.
   * 
   * @param session The client session.
   * @param cause The message that caused the violation.
   * @param diagnosticMessage The diagnostic sent back to the client.
   * @throws NoConnectionException
   */
  private static void disconnectProtocolViolation (IoSession session,
                                                   Message cause,
                                                   String diagnosticMessage,
                                                   Throwable error)
  {
    if (diagnosticMessage == null)
      diagnosticMessage = "Frame format error";
    
    warn ("Disconnecting client " + idFor (session) + 
          " due to protocol violation: " +
          diagnosticMessage, Router.class);

    if (error != null)
      diagnostic ("Decode stack trace", Router.class, error);
    
    Connection connection = peekConnectionFor (session);
    
    if (connection != null)
    {
      connection.lockWrite ();
      
      try
      {
        connection.close ();
      } finally
      {
        connection.unlockWrite ();
      }
    }
    
    // send Disconn and close
    send (session,
          new Disconn (REASON_PROTOCOL_VIOLATION,
                       diagnosticMessage)).addListener (CLOSE);
  }

  /**
   * Send a NACK response for a parse error with error info.
   */
  private static void nackParseError (IoSession session,
                                      XidMessage inReplyTo,
                                      String expr,
                                      ParseException ex)
  {
    int code;
    Object [] args = EMPTY_ARGS;
    String message;
    
    if (ex instanceof ConstantExpressionException)
    {
      code = EXP_IS_TRIVIAL;
      message = ex.getMessage ();
    } else
    {
      code = PARSE_ERROR;
      
      if (ex.currentToken == null)
      {
        // handle ParseException with no token info
        
        message = ex.getMessage ();
        args = new Object [] {0, ""};
      } else
      {
        // use token info to generate a better error message
        
        args = new Object [] {ex.currentToken.next.beginColumn,
                              ex.currentToken.next.image};
        
        /*
         * NB: we could use %1 and %2 to refer to args in the message
         * here, but why make it harder for the client?
         */
        message = "Parse error at column " + args [0] + 
                  ", token \"" + args [1] + "\": expected: " +
                  expectedTokensFor (ex);
      }
    }
    
    diagnostic ("Subscription add/modify for client " + idFor (session) + 
                " failed with parse error: " + message, Router.class);
    diagnostic ("Subscription was: " + expr, Router.class);
    
    send (session, new Nack (inReplyTo, code, message, args));
  }
  
  /**
   * Send a NACK due to a blown limit, e.g. Subscription.Max-Count.
   */
  private static void nackLimit (IoSession session, XidMessage inReplyTo,
                                 String message)
  {
    send (session, new Nack (inReplyTo, IMPL_LIMIT, message));
  }
  
  /**
   * Send a NACK due to an invalid subscription ID.
   */
  private static void nackNoSub (IoSession session, XidMessage inReplyTo,
                                 long subscriptionId, String message)
  {
    send (session, new Nack (inReplyTo, NO_SUCH_SUB, message,
                             subscriptionId));
  }
  
  public void exceptionCaught (IoSession session, Throwable ex)
    throws Exception
  {
    if (ex instanceof IOException)
    {
      diagnostic ("IO exception while processing message from " + 
                  idFor (session) + ": " + shortException (ex), this);
      
      if (shouldLog (TRACE))
        trace ("Trace of IO exception from " + idFor (session), this, ex);
    } else
    {
      alarm ("Server exception", this, ex);
    }
  }
  
  public void messageSent (IoSession session, Object message)
    throws Exception
  {
    // zip
  }

  /**
   * NB this can be called *after* close () is completed sometimes.
   */
  public void sessionClosed (IoSession session)
    throws Exception
  {
    if (shouldLog (DIAGNOSTIC))
      diagnostic ("Session for client " + idFor (session) + " closed", this);
    
    sessions.remove (session);

    Connection connection = peekConnectionFor (session);
    
    if (connection != null)
    {
      connection.lockWrite ();
     
      try
      {
        if (connection.isOpen ())
        {
          diagnostic ("Client " + idFor (session) + 
                      " disconnected without warning", this);

          connection.close ();
        }
      } finally
      {
        connection.unlockWrite ();
      }
    }
  }

  public void sessionCreated (IoSession session)
    throws Exception
  {
    // client has this long to connect or UNotify
    session.getConfig ().setIdleTime
      (READER_IDLE, 
       routerOptions.getInt ("IO.Idle-Connection-Timeout"));
    
    // set defaults for connectionless sessions
    setMaxFrameLengthFor
      (session,
       CONNECTION_OPTION_SET.defaults.getInt ("Packet.Max-Length"));

    setSendQueueDropPolicy 
      (session, 
       CONNECTION_OPTION_SET.defaults.getString ("Send-Queue.Drop-Policy"));
    
    setSendQueueMaxLength 
      (session, 
       CONNECTION_OPTION_SET.defaults.getInt ("Send-Queue.Max-Length"));
    
    sessions.add (session);
  }

  public void sessionIdle (IoSession session, IdleStatus status)
    throws Exception
  {
    // close idle sessions that we haven't seen a ConnRqst for yet
    if (status == READER_IDLE && peekConnectionFor (session) == null)
    {
      diagnostic
        ("Client " + idFor (session) +
         " waited too long to connect: closing session", this);
      
      session.close (false);
    }
  }

  public void sessionOpened (IoSession session)
    throws Exception
  {
    diagnostic ("Client " + hostIdFor (remoteHostAddressFor (session)) + 
                " opened session " + idFor (session) + 
                " for connection on " + session.getServiceAddress () + 
                (isSecure (session) ? " using TLS" : ""), this);
  }
  
  private static void setConnection (IoSession session,
                                     Connection connection)
  {
    session.setAttribute ("connection", connection);
    
    setMaxFrameLengthFor
      (session, connection.options.getInt ("Packet.Max-Length"));
    
    setSendQueueDropPolicy 
      (session, connection.options.getString ("Send-Queue.Drop-Policy"));
    
    setSendQueueMaxLength 
      (session, connection.options.getInt ("Send-Queue.Max-Length"));
  }
  
  /**
   * Get the (open) connection associated with a session or throw
   * NoConnectionException.
   */
  private static Connection connectionFor (IoSession session)
    throws NoConnectionException
  {
    Connection connection = (Connection)session.getAttribute ("connection");
    
    if (connection == null)
      throw new NoConnectionException ("No connection established for session");
    else if (!connection.isOpen ())
      throw new NoConnectionException ("Connection is closed");
    else
      return connection;
  }
  
  /**
   * Like connectionFor () but also acquires a write lock.
   * 
   * @throws NoConnectionException if there is no connection for the
   *           session or the connection is not open.
   */
  private static Connection writeableConnectionFor (IoSession session)
    throws NoConnectionException
  {
    Connection connection = connectionFor (session);
    
    connection.lockWrite ();
    
    if (!connection.isOpen ())
    {
      connection.unlockWrite ();
      
      throw new NoConnectionException ("Connection is closed");
    }
    
    return connection;
  }
  
  private static WriteFuture send (IoSession session, Message message)
  {    
    if (shouldLog (TRACE))
    {
      trace ("Server sent message to " + idFor (session) + ": " + message,
             Router.class);
    }
    
    return session.write (message);
  }

  private void logNotification (IoSession session, Notify message)
  {
    trace ("Notification " + idFor (message) + 
           " from client " + idFor (session) + ":\n" + 
           formatNotification (message.attributes), this);
  }
  
  private static void incrementNotificationCount (List<Subscription> subscriptions)
  {
    for (Subscription subscription : subscriptions)
      subscription.notificationCount++;
  }

  /**
   * Get the connection associated with a session or null for no connection.
   */
  private static Connection peekConnectionFor (IoSession session)
  {
    return (Connection)session.getAttribute ("connection");
  }
  
  /**
   * Test if connection is closed or underlying session is closing.
   */
  private static boolean connectionClosed (IoSession session)
  {
    Connection connection = peekConnectionFor (session);
    
    return session.isClosing () || 
            (connection != null && !connection.isOpen ());
  }

  public static boolean isSecure (IoSession session)
  {
    return session.getFilterChain ().contains (SecurityFilter.class);
  }
}
