package org.avis.router;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import java.io.Closeable;
import java.io.IOException;

import java.net.InetSocketAddress;

import junit.framework.AssertionFailedError;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import org.avis.io.ClientFrameCodec;
import org.avis.io.messages.ConnRply;
import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.DisconnRply;
import org.avis.io.messages.DisconnRqst;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.NotifyEmit;
import org.avis.io.messages.RequestMessage;
import org.avis.io.messages.SubAddRqst;
import org.avis.io.messages.SubRply;
import org.avis.io.messages.XidMessage;
import org.avis.security.Keys;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.avis.io.messages.ConnRqst.EMPTY_OPTIONS;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.trace;
import static org.avis.router.JUTestRouter.PORT;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.util.Text.className;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Basic Avis test client.
 */
public class SimpleClient implements IoHandler, Closeable
{
  protected static final int RECEIVE_TIMEOUT = 5000;
  
  protected NioSocketConnector connector;
  protected IoSession clientSession;
  protected boolean connected;
  protected BlockingQueue<Message> incomingMessages =
    new LinkedBlockingQueue<Message> ();

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
    this (new InetSocketAddress (hostname, port));
    
    this.clientName = clientName;    
  }
  
  public SimpleClient (InetSocketAddress address)
  {
    this (address, defaultFilters ());
  }
  
  public SimpleClient (InetSocketAddress address, 
                       DefaultIoFilterChainBuilder filters)
  {
    this ("client", address, filters);
  }
  
  public SimpleClient (String clientName,
                       InetSocketAddress address, 
                       DefaultIoFilterChainBuilder filters)
  {
    this.clientName = clientName;
    
    connector = new NioSocketConnector ();

    connector.setFilterChainBuilder (filters);
   
    connector.setHandler (this);

    ConnectFuture future = connector.connect (address);
                                     
    future.awaitUninterruptibly ();
    
    clientSession = future.getSession ();
  }
  
  public synchronized void send (Message message)
    throws NoConnectionException
  {
    checkConnected ();
  
    /*
     * todo under MINA 1.1.5 this joining-the-send-future malarkey has
     * no effect: a close after a send can still eat the sent message.
     * This does not seem to be the case in 2.0 This only really
     * affects UNotify because the others all use the Disconn
     * sequence.
     */
     if (!clientSession.write (message).awaitUninterruptibly (RECEIVE_TIMEOUT))
     {
       throw new RuntimeIoException 
         ("Timed out waiting to send " + message.name ());
     }
  }
  
  public Message receive ()
    throws InterruptedException, MessageTimeoutException, NoConnectionException
  {
    return receive (RECEIVE_TIMEOUT);
  }

  public Message receive (long timeout)
    throws MessageTimeoutException, NoConnectionException, InterruptedException
  {
    Message message = incomingMessages.poll (timeout, MILLISECONDS);
    
    if (message == null)
    {
      if (!clientSession.isConnected ())
      {
        throw new MessageTimeoutException
          (clientName + " did not receive a reply: connection is closed");
      } else
      {
        throw new MessageTimeoutException
          (clientName + " did not receive a reply");
      }
    }
    
    return message;
  }

  private void checkConnected ()
    throws NoConnectionException
  {
    if (!clientSession.isConnected ())
      throw new NoConnectionException ("Not connected");
  }
  
  public <R extends XidMessage> R sendAndReceive (RequestMessage<R> request)
    throws Exception
  {
    send (request);
   
    return receiveReply (request);
  }
  
  @SuppressWarnings("unchecked")
  <R extends XidMessage> R receiveReply (RequestMessage<R> request)
    throws Exception
  {
    XidMessage reply = receive (request.replyType ());
    
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
      
      throw new IOException ("NACK reply:" + nack.formattedMessage ());
    } else
    {
      throw new IOException
        ("Protocol error: received a " + className (reply) +
         ": was expecting " + className (request.replyType ()));
    }
  }
  
  public <T extends Message> T receive (Class<T> type)
    throws MessageTimeoutException, InterruptedException, NoConnectionException
  {
    return receive (type, RECEIVE_TIMEOUT);
  }
  
  /**
   * Wait until we receive a message of a given type. Other messages
   * are discarded.
   * @throws NoConnectionException 
   */
  @SuppressWarnings("unchecked")
  public synchronized <T extends Message> T receive (Class<T> type, long timeout)
    throws MessageTimeoutException, InterruptedException, NoConnectionException
  {
    long start = currentTimeMillis ();
    
    while (currentTimeMillis () - start <= timeout)
    {
      Message message = receive (timeout);
      
      if (type.isAssignableFrom (message.getClass ()))
        return (T)message;
    }
    
    throw new MessageTimeoutException
      ("Failed to receive a " + type.getName ());
  }

  public void sendNotify (Map<String, Object> attributes)
    throws Exception
  {
    send (new NotifyEmit (attributes, true, EMPTY_KEYS));
  }
  
  public void sendNotify (Map<String, Object> attributes, Keys keys)
    throws Exception
  {
    send (new NotifyEmit (attributes, false, keys));
  }

  public SubRply subscribe (String subExpr)
    throws Exception
  {
    return subscribe (subExpr, EMPTY_KEYS);
  }
  
  public synchronized SubRply subscribe (String subExpr, Keys keys)
    throws Exception
  {
    SubAddRqst subAddRqst = new SubAddRqst (subExpr, keys, true);
    
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
    
    send (new ConnRqst (4, 0, options, EMPTY_KEYS, EMPTY_KEYS));
    
    Message reply = receive ();
    
    assertTrue (reply instanceof ConnRply);
    connected = true;
    
    return (ConnRply)reply;
  }

  public void close ()
    throws IOException
  {
    try
    {
      close (RECEIVE_TIMEOUT);
    } catch (NoConnectionException ex)
    {
      throw new IOException (ex.getMessage ());
    } catch (MessageTimeoutException ex)
    {
      throw new IOException (ex.getMessage ());
    } catch (InterruptedException ex)
    {
      currentThread ().interrupt ();
    }
  }
  
  public synchronized void close (long timeout) 
    throws NoConnectionException, MessageTimeoutException, InterruptedException
  {
    if (clientSession == null)
      return;
    
    if (connected && clientSession.isConnected ())
    {
      send (new DisconnRqst ());
      receive (DisconnRply.class, timeout);
    }

    try
    {
      if (!clientSession.close ().await (RECEIVE_TIMEOUT))
        throw new RuntimeIoException ("Failed to close client session");
    } finally
    {
      clientSession = null;
    }
    
    connector.dispose ();
  }
  
  /**
   * Close session with no disconnect request.
   */
  public synchronized void closeImmediately ()
  {
    connected = false;
    clientSession.close ();
    clientSession = null;
    connector.dispose ();
  }

  // IoHandler interface

  public void exceptionCaught (IoSession session, Throwable ex) 
    throws Exception
  {
    alarm (clientName + ": client internal exception", this, ex);
  }

  public void messageReceived (IoSession session,
                                            Object message)
    throws Exception
  {
    trace (clientName + ": message received: " + message, this);
    
    incomingMessages.add ((Message)message);
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
  
    private static DefaultIoFilterChainBuilder defaultFilters ()
  {
    DefaultIoFilterChainBuilder filters = new DefaultIoFilterChainBuilder ();
    
    filters.addLast ("codec", ClientFrameCodec.FILTER);
    
    return filters;
  }
}