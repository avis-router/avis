package org.avis.router;

import java.util.concurrent.ExecutorService;

import java.io.Closeable;
import java.io.IOException;

import java.net.InetSocketAddress;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.ReadThrottleFilterBuilder;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import org.avis.config.Options;
import org.avis.io.ClientFrameCodec;
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
import org.avis.io.messages.RequestMessage;
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
import org.avis.util.IllegalOptionException;
import org.avis.util.ListenerList;

import static java.lang.Integer.toHexString;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.identityHashCode;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.apache.mina.common.IdleStatus.READER_IDLE;
import static org.apache.mina.common.IoFutureListener.CLOSE;
import static org.avis.common.Common.CLIENT_VERSION_MAJOR;
import static org.avis.common.Common.CLIENT_VERSION_MINOR;
import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.io.FrameCodec.setMaxFrameLengthFor;
import static org.avis.io.Net.enableTcpNoDelay;
import static org.avis.io.messages.Disconn.REASON_PROTOCOL_VIOLATION;
import static org.avis.io.messages.Disconn.REASON_SHUTDOWN;
import static org.avis.io.messages.Nack.EMPTY_ARGS;
import static org.avis.io.messages.Nack.EXP_IS_TRIVIAL;
import static org.avis.io.messages.Nack.IMPL_LIMIT;
import static org.avis.io.messages.Nack.NOT_IMPL;
import static org.avis.io.messages.Nack.NO_SUCH_SUB;
import static org.avis.io.messages.Nack.PARSE_ERROR;
import static org.avis.io.messages.Nack.PROT_ERROR;
import static org.avis.io.messages.Nack.PROT_INCOMPAT;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.trace;
import static org.avis.logging.Log.warn;
import static org.avis.router.ConnectionOptionSet.CONNECTION_OPTION_SET;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.subscription.parser.SubscriptionParserBase.expectedTokensFor;

public class Router implements IoHandler, Closeable
{
  private static final String ROUTER_VERSION =
    System.getProperty ("avis.router.version", "<unknown>");
  
  private RouterOptions routerOptions;
  private ExecutorService executor;
  private SocketAcceptor acceptor;
  private volatile boolean closing;
  
  /**
   * Server maintains its own concurrently-accessible session set
   * rather than use
   * {@link IoService#getManagedSessions(java.net.SocketAddress)} in
   * order to avoid overhead of copying on every traversal. This may
   * be a premature optimization since the concurrent hash set may
   * have just as large an overhead: if anyone cares they should
   * profile this.
   */
  private ConcurrentHashSet<IoSession> sessions;

  private ListenerList<NotifyListener> notifyListeners;
  private ListenerList<CloseListener> closeListeners;
  
  public Router ()
    throws IOException
  {
    this (DEFAULT_PORT);
  }
  
  public Router (int port)
    throws IOException
  {
    this (new RouterOptions (port));
  }
  
  public Router (RouterOptions options)
    throws IOException, IllegalOptionException
  {
    this.notifyListeners = 
      new ListenerList<NotifyListener>
        (NotifyListener.class, "notifyReceived", Notify.class, Keys.class);
    this.closeListeners = 
      new ListenerList<CloseListener>
        (CloseListener.class, "routerClosing", Router.class);
    
    this.routerOptions = options;
    this.sessions = new ConcurrentHashSet<IoSession> ();
    this.executor = newCachedThreadPool ();
    this.acceptor =
      new SocketAcceptor (getRuntime ().availableProcessors () + 1,
                          executor);
    
    SocketAcceptorConfig acceptorConfig = new SocketAcceptorConfig ();
    
    acceptorConfig.setReuseAddress (true);
    acceptorConfig.setThreadModel (ThreadModel.MANUAL);
    
    /*
     * Setup IO filter chain with codec and then thread pool. NOTE:
     * The read throttling system needs an ExecutorFilter to glom
     * onto: it's not clear that we gain any other benefit from it
     * since notification processing is non-blocking. See
     * http://mina.apache.org/configuring-thread-model.html.
     */
    DefaultIoFilterChainBuilder filterChainBuilder =
      acceptorConfig.getFilterChain ();

    filterChainBuilder.addLast ("codec", ClientFrameCodec.FILTER);
    
    filterChainBuilder.addLast
      ("threadPool", new ExecutorFilter (executor));
    
    for (InetSocketAddress address : options.listenAddresses ())
    {
      diagnostic ("Router binding to address: " + address, this);

      acceptor.bind (address, this, acceptorConfig);
    }
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
    closeListeners = null;
    
    Disconn disconnMessage = new Disconn (REASON_SHUTDOWN);
    
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
          }
        } finally
        {
          connection.unlockWrite ();
        }
      }
    }
    
    sessions.clear ();
    acceptor.unbindAll ();
    executor.shutdown ();
    
    try
    {
      if (!executor.awaitTermination (15, SECONDS))
        warn ("Failed to cleanly shut down thread pool", this);
    } catch (InterruptedException ex)
    {
      diagnostic ("Interrupted while waiting for shutdown", this, ex);
    }
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
  
  /**
   * The shared executor thread pool used by the router. Plugins may
   * share this.
   */
  public ExecutorService executor ()
  {
    return executor;
  }
  
  /**
   * The router's MINA socket acceptor. Plugins may share this.
   */
  public SocketAcceptor socketAcceptor ()
  {
    return acceptor;
  }
  
  public Options options ()
  {
    return routerOptions;
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
    closeListeners.add (listener);
  }
  
  /**
   * Undo the effect of {@link #addCloseListener(CloseListener)}.
   */
  public void removeCloseListener (CloseListener listener)
  {
    closeListeners.remove (listener);
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
    notifyListeners.add (listener);
  }
  
  /**
   * Undo the effect of {@link #addNotifyListener(NotifyListener)}.
   */
  public void removeNotifyListener (NotifyListener listener)
  {
    notifyListeners.remove (listener);
  }
  
  // IoHandler interface

  public void messageReceived (IoSession session, Object messageObject)
    throws Exception
  {
    if (closing)
      return;
    
    if (shouldLog (TRACE))
      trace ("Server got message from " + idFor (session) +
             ": " + messageObject, this);
    
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
      handleProtocolViolation (session, message, ex.getMessage (), ex);
    }
  }

  private void handleConnRqst (IoSession session, ConnRqst message)
    throws ProtocolCodecException
  {
    if (peekConnectionFor (session) != null)
      throw new ProtocolCodecException ("Already connected");
    
    Connection connection =
      new Connection (routerOptions, message.options,
                      message.subscriptionKeys, message.notificationKeys);
    
    int maxKeys = connection.options.getInt ("Connection.Max-Keys");
    
    if (message.versionMajor != CLIENT_VERSION_MAJOR ||
        message.versionMinor > CLIENT_VERSION_MINOR)
    {
      send (session,
            new Nack (message, PROT_INCOMPAT,
                      "Max supported protocol version is " +
                       + CLIENT_VERSION_MAJOR + '.' + CLIENT_VERSION_MINOR));
    } else if (message.notificationKeys.size () > maxKeys ||
               message.subscriptionKeys.size () > maxKeys)
    {
      nackLimit (session, message, "Too many keys");
    } else
    {
      updateTcpSendImmediately (session, connection.options);
      updateQueueLength (session, connection);
      
      connection.options.setWithLegacy
        ("Vendor-Identification", "Avis " + ROUTER_VERSION);
      
      connection.lockWrite ();
      
      try
      {
        setConnection (session, connection);
        
        send (session, new ConnRply (message, connection.options.accepted ()));
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
      
      Keys newKeys = subscription.keys.delta (message.addKeys, message.delKeys);

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
    deliverNotification (message, connectionFor (session).notificationKeys);
  }
  
  private void handleUnotify (UNotify message)
  {
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
          send (session, new NotifyDeliver (message.attributes,
                                            matches.secure (),
                                            matches.insecure ()));
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
    if (session.getScheduledWriteRequests () == 0)
      send (session, ConfConn.INSTANCE);
  }
  
  private static void handleQuench (IoSession session,
                                    QuenchPlaceHolder message)
  {
    diagnostic
      ("Rejecting quench request from client: quench is not supported",
       Router.class);
    
    send (session, new Nack (message, NOT_IMPL, "Quench not supported"));
  }

  private static void handleError (IoSession session, ErrorMessage message)
  {
    /*
     * When a frame is too large: for requests we can send a NACK for,
     * simply reject and keep on truckin. For frames that cannot be
     * NACK'd (e.g. NotifyEmit), we treat this as a protocol error and
     * Disconn with a descriptive message rather than silently (from
     * the client's POV) dropping them.
     */
    if (message.error instanceof FrameTooLargeException)
    {
      if (message.cause instanceof RequestMessage<?>)
      {
        // codec will have suspended input on error, restart
        session.resumeRead ();
        
        nackLimit (session, (XidMessage)message.cause, 
                   message.error.getMessage ());
      } else
      {
        handleProtocolViolation (session, message.cause, 
                                 message.error.getMessage (), null);
      }
    } else
    {
      handleProtocolViolation (session, message.cause, 
                               message.formattedMessage (), message.error);
    }
  }

  /**
   * Handle a protocol violation by a client by sending a NACK (if
   * appropriate) and disconnecting with the REASON_PROTOCOL_VIOLATION code.
   * 
   * @param session The client session.
   * @param cause The message that caused the violation.
   * @param diagnosticMessage The diagnostic sent back to the client.
   * @throws NoConnectionException 
   */
  private static void handleProtocolViolation (IoSession session,
                                               Message cause,
                                               String diagnosticMessage,
                                               Throwable error)
  {
    warn ("Disconnecting client due to protocol violation: " +
          diagnosticMessage, Router.class);

    if (error != null)
      diagnostic ("Decode stack trace", Router.class, error);
    
    session.suspendRead ();

    Connection connection = peekConnectionFor (session);
    
    if (connection != null)
      connection.close ();
    
    if (cause instanceof RequestMessage<?>)
    {
      send (session,
            new Nack ((XidMessage)cause, PROT_ERROR, diagnosticMessage));
    }
    
    // send Disconn and close
    send (session,
          new Disconn (REASON_PROTOCOL_VIOLATION,
                       diagnosticMessage)).addListener (CLOSE);
  }

  /**
   * Handle the TCP.Send-Immediately connection option if set.
   */
  private static void updateTcpSendImmediately (IoSession session,
                                                Options options)
  {
    if (!enableTcpNoDelay (session, 
                           options.getInt ("TCP.Send-Immediately") == 1))
    {
      options.remove ("TCP.Send-Immediately"); 
    }
  }
  
  /**
   * Update the receive/send queue lengths based on connection
   * options. Currently only implements Receive-Queue.Max-Length using
   * MINA's ReadThrottleFilterBuilder filter.
   */
  private static void updateQueueLength (IoSession session,
                                         Connection connection)
  {
    ReadThrottleFilterBuilder readThrottle =
      (ReadThrottleFilterBuilder)session.getAttribute ("readThrottle");
    
    readThrottle.setMaximumConnectionBufferSize
      (connection.options.getInt ("Receive-Queue.Max-Length"));
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
    
    diagnostic ("Subscription add/modify failed with parse error: " +
                message, Router.class);
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
    alarm ("Server exception", this, ex);
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
    if (shouldLog (TRACE))
      trace ("Server session " + idFor (session) + " closed", this);
    
    sessions.remove (session);

    Connection connection = peekConnectionFor (session);
    
    if (connection != null)
    {
      connection.lockWrite ();
     
      try
      {
        if (connection.isOpen ())
        {
          diagnostic ("Client disconnected without warning", this);
          
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
    // set idle time to 15 seconds: client has this long to connect or UNotify
    // todo idle time should be configurable
    session.setIdleTime (READER_IDLE, 15);
    
    // install read throttle
    ReadThrottleFilterBuilder readThrottle = new ReadThrottleFilterBuilder ();
    readThrottle.setMaximumConnectionBufferSize
      (CONNECTION_OPTION_SET.defaults.getInt ("Receive-Queue.Max-Length"));
    
    readThrottle.attach (session.getFilterChain ());
    
    session.setAttribute ("readThrottle", readThrottle);
    
    // set default max length for connectionless sessions
    setMaxFrameLengthFor
      (session,
       CONNECTION_OPTION_SET.defaults.getInt ("Packet.Max-Length"));
    
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
      
      session.close ();
    }
  }

  public void sessionOpened (IoSession session)
    throws Exception
  {
    trace ("Server session opened for " + idFor (session), this);
  }
  
  private static void setConnection (IoSession session,
                                     Connection connection)
  {
    session.setAttachment (connection);
    
    setMaxFrameLengthFor
      (session, connection.options.getInt ("Packet.Max-Length"));
  }
  
  /**
   * Get the (open) connection associated with a session or throw
   * NoConnectionException.
   */
  private static Connection connectionFor (IoSession session)
    throws NoConnectionException
  {
    Connection connection = (Connection)session.getAttachment ();
    
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
  
  /**
   * Get the connection associated with a session or null for no connection.
   */
  private static Connection peekConnectionFor (IoSession session)
  {
    return (Connection)session.getAttachment ();
  }
  
  private static String idFor (IoSession session)
  {
    return toHexString (identityHashCode (session));
  }
}
