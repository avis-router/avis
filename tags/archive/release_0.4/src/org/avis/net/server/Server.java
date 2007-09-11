package org.avis.net.server;

import java.util.Set;

import java.io.IOException;

import java.net.SocketException;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.TransportType;
import org.apache.mina.io.socket.SocketSessionConfig;
import org.apache.mina.protocol.ProtocolCodecFactory;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;
import org.apache.mina.protocol.codec.DemuxingProtocolCodecFactory;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;
import org.apache.mina.registry.SimpleServiceRegistry;

import org.avis.net.FrameCodec;
import org.avis.net.messages.ConfConn;
import org.avis.net.messages.ConnRply;
import org.avis.net.messages.ConnRqst;
import org.avis.net.messages.Disconn;
import org.avis.net.messages.DisconnRply;
import org.avis.net.messages.DisconnRqst;
import org.avis.net.messages.Message;
import org.avis.net.messages.Nack;
import org.avis.net.messages.Notify;
import org.avis.net.messages.NotifyDeliver;
import org.avis.net.messages.NotifyEmit;
import org.avis.net.messages.QuenchPlaceHolder;
import org.avis.net.messages.SecRply;
import org.avis.net.messages.SecRqst;
import org.avis.net.messages.SubAddRqst;
import org.avis.net.messages.SubDelRqst;
import org.avis.net.messages.SubModRqst;
import org.avis.net.messages.SubRply;
import org.avis.net.messages.TestConn;
import org.avis.net.messages.UNotify;
import org.avis.net.messages.XidMessage;
import org.avis.net.security.Keys;
import org.avis.pubsub.parser.ParseException;
import org.avis.util.ConcurrentHashSet;

import static dsto.dfc.logging.Log.DIAGNOSTIC;
import static dsto.dfc.logging.Log.alarm;
import static dsto.dfc.logging.Log.diagnostic;
import static dsto.dfc.logging.Log.isEnabled;
import static dsto.dfc.logging.Log.warn;

import static org.apache.mina.common.IdleStatus.READER_IDLE;

import static org.avis.net.messages.Disconn.REASON_SHUTDOWN;
import static org.avis.net.messages.Nack.NO_SUCH_SUB;
import static org.avis.net.messages.Nack.PARSE_ERROR;
import static org.avis.net.messages.Nack.PROT_ERROR;
import static org.avis.net.security.Keys.EMPTY_KEYS;

public class Server implements ProtocolProvider, ProtocolHandler
{
  private static final String ROUTER_VERSION;
  private static final DemuxingProtocolCodecFactory CODEC_FACTORY;
  
  static
  {
    CODEC_FACTORY =
      new DemuxingProtocolCodecFactory ();
    CODEC_FACTORY.register (FrameCodec.class);
    
    ROUTER_VERSION = System.getProperty ("avis.router.version", "<unknown>");
  }
  
  private ServiceRegistry registry;
  // todo surely we don't have to track sessions ourselves?
  private Set<ProtocolSession> sessions;

  public Server (int port)
    throws IOException
  {
    sessions = new ConcurrentHashSet<ProtocolSession> ();
    registry = new SimpleServiceRegistry ();

    registry.bind
      (new Service ("avis", TransportType.SOCKET, port), this);
  }

  /**
   * Close all connections synchronously. May be called more than once.
   */
  public void close ()
  {
    synchronized (this)
    {
      if (registry == null)
        return;
      
      registry.unbindAll ();
      registry = null;
    }
    
    Disconn disconnMessage = new Disconn (REASON_SHUTDOWN, "");
    
    for (ProtocolSession session : sessions)
    {
      Connection connection = peekConnectionFor (session);
      
      if (connection != null)
      {
        connection.lockWrite ();

        try
        {
          if (connection.isOpen ())
          {
            session.write (disconnMessage);
            connection.close ();
          }
        } finally
        {
          connection.unlockWrite ();
        }
      }
      
      session.close (true);
    }
    
    sessions.clear ();
    sessions = null;
    registry = null;
  }

  public ProtocolCodecFactory getCodecFactory ()
  {
    return CODEC_FACTORY;
  }

  public ProtocolHandler getHandler ()
  {
    return this;
  }
  
  private static void setConnection (ProtocolSession session,
                                     Connection connection)
  {
    session.setAttachment (connection);
  }
  
  /**
   * Get the (open) connection associated with a session or throw
   * NoConnectionException.
   */
  private static Connection connectionFor (ProtocolSession session)
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
  private static Connection writeableConnectionFor (ProtocolSession session)
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
  private static Connection peekConnectionFor (ProtocolSession session)
  {
    return (Connection)session.getAttachment ();
  }

  // ProtocolHandler interface

  public void messageReceived (ProtocolSession session, Object messageObject)
    throws Exception
  {
    if (isEnabled (DIAGNOSTIC))
      diagnostic ("Server got message: " + messageObject, this);
    
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
        case QuenchPlaceHolder.ID:
          handleQuench (session, (QuenchPlaceHolder)message);
          break;
        default:
          warn
            ("Server got an unhandleable message type: " + message, this);
      }
    } catch (ProtocolViolationException ex)
    {
      diagnostic ("Client protocol violation: " + ex.getMessage (), this);
      
      if (message instanceof XidMessage)
      {
        session.write
          (new Nack ((XidMessage)message, PROT_ERROR, ex.getMessage ()));
      }
    }
  }

  private void handleConnRqst (ProtocolSession session, ConnRqst message)
    throws ProtocolViolationException
  {
    if (peekConnectionFor (session) != null)
      throw new ProtocolViolationException ("Already connected");
    
    Connection connection =
      new Connection (message.options,
                      message.subscriptionKeys, message.notificationKeys);
    
    // todo support other standard connection options
    updateCoalesceDelay (session, connection);
    
    connection.options.put ("Vendor-Identification", "Avis " + ROUTER_VERSION);
    
    connection.lockWrite ();
    
    try
    {
      setConnection (session, connection);
      
      session.write (new ConnRply (message, connection.options.accepted ()));
    } finally
    {
      connection.unlockWrite ();
    }
  }

  /**
   * NOTE: the spec says it's a violation to add the same key more
   * than once or to remove a non-existent key (sec 7.4.8) and
   * should be reported as a protocol violation. Avis currently
   * doesn't enforce this since neither of these cases has any
   * effect on its key collections and the check would add overhead.
   */
  private void handleSecRqst (ProtocolSession session, SecRqst message)
    throws NoConnectionException
  {
    Connection connection = writeableConnectionFor (session);
    
    try
    {
      connection.notificationKeys =
        connection.notificationKeys.delta
          (message.addNtfnKeys, message.delNtfnKeys);
  
      connection.subscriptionKeys =
        connection.subscriptionKeys.delta
          (message.addSubKeys, message.delSubKeys);
      
      session.write (new SecRply (message));
    } finally
    {
      connection.unlockWrite ();
    }
  }

  private void handleDisconnRqst (ProtocolSession session, DisconnRqst message)
    throws NoConnectionException
  {
    Connection connection = writeableConnectionFor (session);

    try
    {
      connection.close ();
      
      session.write (new DisconnRply (message));
    } finally
    {
      connection.unlockWrite ();
    }

    session.close ();
  }
  
  private void handleSubAddRqst (ProtocolSession session, SubAddRqst message)
    throws NoConnectionException
  {
    Connection connection = writeableConnectionFor (session);

    try
    {
      Subscription subscription =
        new Subscription (message.subscriptionExpr,
                          message.keys, message.acceptInsecure);
     
      connection.addSubscription (subscription);

      session.write (new SubRply (message, subscription.id));
    } catch (ParseException ex)
    {
      diagnostic ("Subscription add failed with parse error: " +
                  ex.getMessage (), this);
      diagnostic ("Subscription was: " + message.subscriptionExpr, this);
      
      session.write (nackParseError (message, ex));
    } finally
    {
      connection.unlockWrite ();
    }
  }

  private void handleSubModRqst (ProtocolSession session, SubModRqst message)
    throws NoConnectionException  
  {
    Connection connection = writeableConnectionFor (session);

    try
    {
      Subscription subscription =
        connection.subscriptionFor (message.subscriptionId);
      
      if (message.subscriptionExpr.length () > 0)
        subscription.updateExpression (message.subscriptionExpr);

      subscription.keys =
        subscription.keys.delta (message.addKeys, message.delKeys);
      
      session.write (new SubRply (message, subscription.id));
    } catch (ParseException ex)
    {
      diagnostic ("Subscription modify failed with parse error: " +
                  ex.getMessage (), this);
      diagnostic ("Subscription was: " + message.subscriptionExpr, this);
      
      session.write (nackParseError (message, ex));
    } catch (InvalidSubscriptionException ex)
    {
      session.write (new Nack (message, NO_SUCH_SUB, ex.getMessage (),
                               message.subscriptionId));
    } finally
    {
      connection.unlockWrite ();
    }
  }
  
  private void handleSubDelRqst (ProtocolSession session, SubDelRqst message)
    throws NoConnectionException
  {
    Connection connection = writeableConnectionFor (session);
    
    try
    {
      if (connection.removeSubscription (message.subscriptionId) != null)
        session.write (new SubRply (message, message.subscriptionId));
      else
        session.write (new Nack (message, NO_SUCH_SUB,
                                 "Invalid subscription ID",
                                 message.subscriptionId));
    } finally
    {
      connection.unlockWrite ();
    }
  }
  
  private void handleNotifyEmit (ProtocolSession session, NotifyEmit message)
    throws NoConnectionException
  {
    deliverNotification (message, connectionFor (session).notificationKeys);
  }
  
  private void handleUnotify (UNotify message)
  {
    deliverNotification (message, EMPTY_KEYS);
  }

  /**
   * Deliver a notification message to subscribers.
   * 
   * @param message The message (a UNotify or a NotifyEmit).
   * @param notificationKeys The global notification keys that apply
   *          to message. These are in addition to any keys attached
   *          to the message itself.
   */
  private void deliverNotification (Notify message, Keys notificationKeys)
  {
    for (ProtocolSession session : sessions)
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
          session.write (new NotifyDeliver (message.attributes,
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
  }

  private void handleTestConn (ProtocolSession session)
  {
    // if no other outgoing messages are waiting, send a confirm message
    if (session.getScheduledWriteRequests () == 0)
      session.write (new ConfConn ());
  }
  
  private void handleQuench (ProtocolSession session, QuenchPlaceHolder message)
  {
    // TODO implement quench support here
    diagnostic
      ("Rejecting quench request from client: quench is not supported", this);
    
    session.write (new Nack (message, PROT_ERROR, "Quench not supported"));
  }

  /**
   * Handle the Network.Coalesce-Delay/router.coalesce-delay
   * connection option if set.
   */
  private static void updateCoalesceDelay (ProtocolSession session,
                                           Connection connection)
  {
    if (session.getConfig () instanceof SocketSessionConfig)
    {
      int coalesceDelay =
        connection.options.getInt ("Network.Coalesce-Delay");
      
      try
      {
        ((SocketSessionConfig)session.getConfig ()).setTcpNoDelay
          (coalesceDelay == 0);
      } catch (SocketException ex)
      {
        warn ("Failed to set TCP nodelay", Server.class, ex);
        
        connection.options.remove ("Network.Coalesce-Delay"); 
      }
    } else
    {
      connection.options.remove ("Network.Coalesce-Delay"); 
    }
  }

  /**
   * Create a NACK response for a parse error with error info.
   * 
   * todo should provide better error info (see sec 7.4.2 and 6.3)
   */
  private static Nack nackParseError (XidMessage inReplyTo, ParseException ex)
  {
    return new Nack (inReplyTo, PARSE_ERROR, ex.getMessage (), 0, "");
  }
  
  public void exceptionCaught (ProtocolSession session, Throwable ex)
    throws Exception
  {
    warn ("Server exception", this, ex);
  }
  
  public void messageSent (ProtocolSession session, Object message)
    throws Exception
  {
    if (isEnabled (DIAGNOSTIC))
      diagnostic ("Server sent message: " + message, this);
  }

  public void sessionClosed (ProtocolSession session)
    throws Exception
  {
    diagnostic ("Server session closed", this);
    
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

  public void sessionCreated (ProtocolSession session)
    throws Exception
  {
    // set idle time for readers to 30 seconds: client has this long to connect
    session.getConfig ().setIdleTime (READER_IDLE, 30);
    
    sessions.add (session);
  }

  public void sessionIdle (ProtocolSession session, IdleStatus status)
    throws Exception
  {
    // close idle sessions that we haven't seen a ConnRqst for yet
    if (status == READER_IDLE && peekConnectionFor (session) == null)
    {
      diagnostic
        ("Client waited too long to connect: closing session", this);
      
      session.close ();
    }
  }

  public void sessionOpened (ProtocolSession session)
    throws Exception
  {
    diagnostic ("Server session opened", this);
  }
}
