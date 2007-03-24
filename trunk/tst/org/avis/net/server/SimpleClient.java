package org.avis.net.server;

import java.util.Map;

import java.io.IOException;

import java.net.InetSocketAddress;

import junit.framework.AssertionFailedError;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.common.Notification;
import org.avis.net.common.FrameCodec;
import org.avis.net.messages.ConnRply;
import org.avis.net.messages.ConnRqst;
import org.avis.net.messages.DisconnRply;
import org.avis.net.messages.DisconnRqst;
import org.avis.net.messages.Message;
import org.avis.net.messages.Nack;
import org.avis.net.messages.NotifyEmit;
import org.avis.net.messages.SubAddRqst;
import org.avis.net.messages.SubRply;
import org.avis.net.security.Keys;

import static dsto.dfc.logging.Log.alarm;
import static dsto.dfc.logging.Log.trace;

import static java.lang.System.currentTimeMillis;

import static org.avis.net.messages.ConnRqst.EMPTY_OPTIONS;
import static org.avis.net.security.Keys.EMPTY_KEYS;
import static org.avis.net.server.JUTestServer.PORT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Basic Avis test client.
 */
class SimpleClient implements IoHandler
{
  protected static final int RECEIVE_TIMEOUT = 5000;
  
  protected IoSession clientSession;
  protected boolean connected;
  protected volatile Message lastReceived;

  public String clientName;
  
  public SimpleClient ()
    throws IOException
  {
    this ("localhost", PORT);
  }

  public SimpleClient (String clientName)
    throws IOException
  {
    this (clientName, "localhost", PORT);
  }
  
  public SimpleClient (String hostname, int port)
    throws IOException
  {
    this ("client", hostname, port);
  }
  
  public SimpleClient (String clientName, String hostname, int port)
    throws IOException
  {
    this.clientName = clientName;
    
    SocketConnector connector = new SocketConnector ();

    /* Change the worker timeout to 1 second to make the I/O thread
     * quit soon when there's no connection to manage. */
    connector.setWorkerTimeout (1);
    
    SocketConnectorConfig cfg = new SocketConnectorConfig ();
    cfg.setConnectTimeout (10);
    
    DemuxingProtocolCodecFactory codecFactory =
      new DemuxingProtocolCodecFactory ();
    codecFactory.register (FrameCodec.class);
    
    cfg.getFilterChain ().addLast
      ("codec", new ProtocolCodecFilter (codecFactory));
    
    ConnectFuture future =
      connector.connect (new InetSocketAddress (hostname, port),
                         this, cfg);
                                     
    future.join ();
    clientSession = future.getSession ();
  }

  public synchronized void send (Message message)
    throws NoConnectionException
  {
    checkConnected ();
    clientSession.write (message);
  }
  
  public Message receive ()
    throws InterruptedException, MessageTimeoutException, NoConnectionException
  {
    return receive (RECEIVE_TIMEOUT);
  }

  public synchronized Message receive (long timeout)
    throws InterruptedException, MessageTimeoutException,
           NoConnectionException
  {
    if (lastReceived == null)
      wait (timeout);

    if (lastReceived != null)
    {
      Message message = lastReceived;
      
      lastReceived = null;
      
      return message;
    } else
    {
      // may have failed because we are simply not connected any more
      checkConnected ();
      
      throw new MessageTimeoutException
        (clientName + " did not receive a reply");
    }
  }

  private void checkConnected ()
    throws NoConnectionException
  {
    if (!clientSession.isConnected ())
      throw new NoConnectionException ("Not connected");
  }
  
  public Message receive (Class<? extends Message> type)
    throws MessageTimeoutException, InterruptedException, NoConnectionException
  {
    return receive (type, RECEIVE_TIMEOUT);
  }
  
  /**
   * Wait until we receive a message of a given type. Other messages
   * are discarded.
   * @throws NoConnectionException 
   */
  public synchronized Message receive (Class<? extends Message> type, long timeout)
    throws MessageTimeoutException, InterruptedException, NoConnectionException
  {
    long start = currentTimeMillis ();
    
    while (currentTimeMillis () - start <= timeout)
    {
      Message message = receive (timeout);
      
      if (type.isAssignableFrom (message.getClass ()))
        return message;
    }
    
    throw new MessageTimeoutException
      ("Failed to receive a " + type.getName ());
  }

  public void sendNotify (Notification ntfn)
    throws Exception
  {
    send (new NotifyEmit (ntfn, true, EMPTY_KEYS));
  }
  
  public void sendNotify (Notification ntfn, Keys keys)
    throws Exception
  {
    send (new NotifyEmit (ntfn, false, keys));
  }

  public SubRply subscribe (String subExpr)
    throws Exception
  {
    return subscribe (subExpr, EMPTY_KEYS);
  }
  
  public synchronized SubRply subscribe (String subExpr, Keys keys)
    throws Exception
  {
    SubAddRqst subAddRqst = new SubAddRqst (subExpr, keys);
    
    send (subAddRqst);
    
    Message reply = receive ();
    
    if (reply instanceof SubRply)
    {
      SubRply subRply = (SubRply)reply;
      
      assertEquals (subAddRqst.xid, subRply.xid);
      
      return subRply;
    } else if (reply instanceof Nack)
    {
      throw new AssertionFailedError
        (clientName + ": subscription NACK: " + ((Nack)reply).message + ": " + subExpr);
    } else
    {
      throw new AssertionFailedError
        (clientName + ": unexpected reply type: " + reply.getClass ().getName ());
    }
  }

  public ConnRply connect ()
    throws Exception
  {
    return connect (EMPTY_OPTIONS);
  }
  
  public synchronized ConnRply connect (Map<String, Object> options)
    throws Exception
  {
    checkConnected ();
    
    send (new ConnRqst (4, 0, options));
    
    Message reply = receive ();
    
    assertTrue (reply instanceof ConnRply);
    connected = true;
    
    return (ConnRply)reply;
  }

  public void close ()
    throws Exception
  {
    close (RECEIVE_TIMEOUT);
  }
  
  public synchronized void close (long timeout)
    throws Exception
  {
    if (connected && clientSession.isConnected ())
    {
      send (new DisconnRqst ());
      receive (DisconnRply.class, timeout);
    }
    
    clientSession.close ().join ();
    clientSession = null;
  }
  
  /**
   * Close session with no disconnect request.
   */
  public synchronized void closeImmediately ()
  {
    connected = false;
    clientSession.close ();
    clientSession = null;
  }

  // IoHandler interface

  public void exceptionCaught (IoSession session, Throwable ex) 
    throws Exception
  {
    alarm (clientName + ": client internal exception", this, ex);
  }

  public synchronized void messageReceived (IoSession session,
                                            Object message)
    throws Exception
  {
    if (lastReceived != null)
      throw new AssertionFailedError
        (clientName + ": uncollected reply: " + lastReceived +
         " (replacement is " + message + ")");
    
    trace (clientName + ": message received: " + message, this);
    
    lastReceived = (Message)message;
    
    notifyAll ();
  }

  public void messageSent (IoSession session, Object message) throws Exception
  {
    // zip
  }

  public void sessionClosed (IoSession session) throws Exception
  {
    // zip
  }

  public void sessionCreated (IoSession session) throws Exception
  {
    // zip
  }

  public void sessionIdle (IoSession session, IdleStatus status) throws Exception
  {
    // zip
  }

  public void sessionOpened (IoSession session) throws Exception
  {
    // zip
  }
}