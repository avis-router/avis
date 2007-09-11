package org.avis.net.server;

import java.io.IOException;

import java.net.InetSocketAddress;

import junit.framework.AssertionFailedError;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.io.filter.IoThreadPoolFilter;
import org.apache.mina.io.socket.SocketConnector;
import org.apache.mina.protocol.ProtocolCodecFactory;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.codec.DemuxingProtocolCodecFactory;
import org.apache.mina.protocol.filter.ProtocolThreadPoolFilter;
import org.apache.mina.protocol.io.IoProtocolConnector;

import org.avis.Notification;
import org.avis.net.FrameCodec;
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

import dsto.dfc.logging.Log;

import static org.avis.net.security.Keys.EMPTY_KEYS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Basic Avis test client.
 */
class SimpleClient implements ProtocolProvider, ProtocolHandler
{
  private static final DemuxingProtocolCodecFactory CODEC_FACTORY;
  
  private ProtocolSession clientSession;
  private boolean connected;
  private volatile Message lastReply;

  public String clientName;
  
  static
  {
    CODEC_FACTORY =
      new DemuxingProtocolCodecFactory ();
    CODEC_FACTORY.register (FrameCodec.class);
  }
  
  public SimpleClient ()
    throws IOException
  {
    this ("localhost", JUTestServer.PORT);
  }

  public SimpleClient (String clientName)
    throws IOException
  {
    this (clientName, "localhost", JUTestServer.PORT);
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
    
    // Create I/O and Protocol thread pool filter.
    // I/O thread pool performs encoding and decoding of messages.
    // Protocol thread pool performs actual protocol flow.
    IoThreadPoolFilter ioThreadPoolFilter = new IoThreadPoolFilter ();
    ProtocolThreadPoolFilter protocolThreadPoolFilter = new ProtocolThreadPoolFilter ();

    // and start both.
    ioThreadPoolFilter.start ();
    protocolThreadPoolFilter.start ();

    IoProtocolConnector connector =
      new IoProtocolConnector (new SocketConnector ());
    connector.getIoConnector ().getFilterChain ().addFirst
      ("threadPool", ioThreadPoolFilter);
    connector.getFilterChain ().addFirst
      ("threadPool", protocolThreadPoolFilter);

    clientSession =
      connector.connect (new InetSocketAddress (hostname, port), 10, this);
  }
  
  public ProtocolCodecFactory getCodecFactory ()
  {
    return CODEC_FACTORY;
  }
  
  public ProtocolHandler getHandler ()
  {
    return this;
  }
  
  public void send (Message message)
  {
    clientSession.write (message);
  }
  
  public Message receive ()
    throws InterruptedException, MessageTimeoutException
  {
    return receive (5000);
  }

  public synchronized Message receive (int timeout)
    throws InterruptedException, MessageTimeoutException
  {
    if (lastReply == null)
      wait (timeout);
    
    if (lastReply != null)
    {
      Message message = lastReply;
      
      lastReply = null;
      
      return message;
    } else
    {
      throw new MessageTimeoutException
        (clientName + " did not receive a reply");
    }
  }

  public void notify (Notification ntfn, boolean deliverInsecure)
    throws Exception
  {
    emitNotify (ntfn, deliverInsecure);
  }

  public void emitNotify (Notification ntfn, boolean deliverInsecure)
    throws Exception
  {
    send (new NotifyEmit (ntfn, deliverInsecure, ntfn.keys));
  }

  public void subscribe (String subExpr)
    throws Exception
  {
    subscribe (subExpr, EMPTY_KEYS);
  }
  
  public void subscribe (String subExpr, Keys keys)
    throws Exception
  {
    SubAddRqst subAddRqst = new SubAddRqst (subExpr, keys);
    
    send (subAddRqst);
    
    Message reply = receive ();
    
    if (reply instanceof SubRply)
    {
      assertEquals (subAddRqst.xid, ((SubRply)reply).xid);
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

  public void connect ()
    throws Exception
  {
    send (new ConnRqst (4, 0));
    assertTrue (receive () instanceof ConnRply);
    connected = true;
  }

  public void close ()
    throws Exception
  {
    if (connected)
    {
      send (new DisconnRqst ());
      assertTrue (receive () instanceof DisconnRply);
    }
    
    clientSession.close ();
    clientSession = null;
  }

  // ProtocolHandler interface
  
  public void exceptionCaught (ProtocolSession session, Throwable ex) 
    throws Exception
  {
    Log.alarm (clientName + ": client internal exception", this, ex);
  }

  public synchronized void messageReceived (ProtocolSession session,
                                            Object message)
    throws Exception
  {
    if (lastReply != null)
      throw new AssertionFailedError
        (clientName + ": uncollected reply: " + lastReply +
         " (replacement is " + message + ")");
    
    Log.trace (clientName + ": message received: " + message, this);
    
    lastReply = (Message)message;
    
    notifyAll ();
  }

  public void messageSent (ProtocolSession session, Object message) throws Exception
  {
    // zip
  }

  public void sessionClosed (ProtocolSession session) throws Exception
  {
    // zip
  }

  public void sessionCreated (ProtocolSession session) throws Exception
  {
    // zip
  }

  public void sessionIdle (ProtocolSession session, IdleStatus status) throws Exception
  {
    // zip
  }

  public void sessionOpened (ProtocolSession session) throws Exception
  {
    // zip
  }
}