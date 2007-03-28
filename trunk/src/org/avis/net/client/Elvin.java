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
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.common.Notification;
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
import org.avis.net.messages.SubAddRqst;
import org.avis.net.messages.SubRply;
import org.avis.net.messages.XidMessage;

import dsto.dfc.logging.Log;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static org.avis.common.Common.CLIENT_VERSION_MAJOR;
import static org.avis.common.Common.CLIENT_VERSION_MINOR;
import static org.avis.net.client.ElvinURI.defaultProtocol;

import static dsto.dfc.logging.Log.alarm;
import static dsto.dfc.logging.Log.warn;

public class Elvin
{
  private static final int RECEIVE_TIMEOUT = 5000;
  
  private ElvinURI elvinUri;
  private IoSession clientSession;
  private ExecutorService executor;
  private boolean connected;
  private Map<Long, Subscription> subscriptions;
  
  /** This is effectively a single-item queue for handling responses
      to XID-based requests. It's volatile since it's used for inter-
      thread communication. */
  protected volatile Message lastReceived;

  public Elvin (String elvinUri)
    throws URISyntaxException, IllegalArgumentException,
           ConnectException, IOException
  {
    this (new ElvinURI (elvinUri));
  }
  
  public Elvin (ElvinURI elvinUri)
    throws IllegalArgumentException, ConnectException, IOException
  {
    this.elvinUri = elvinUri;
    this.subscriptions = new HashMap<Long, Subscription> ();
    
    if (!elvinUri.protocol.equals (defaultProtocol ()))
      throw new IllegalArgumentException
        ("Only the default protocol stack (" +
         defaultProtocol () + ") is currently supported");
    
    openConnection ();
    
    sendAndReceive (new ConnRqst (CLIENT_VERSION_MAJOR, CLIENT_VERSION_MINOR),
                    ConnRply.class);
    
    connected = true;
    
    // todo check connection options
  }

  private void openConnection ()
    throws IOException
  {
    try
    {
      SocketConnector connector = new SocketConnector ();

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
      
      executor = newSingleThreadExecutor ();
      
      filterChainBuilder.addLast
        ("threadPool", new ExecutorFilter (executor));
      
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
    if (clientSession != null && clientSession.isConnected ())
    {
      if (connected)
      {
        try
        {
          sendAndReceive (new DisconnRqst (), DisconnRply.class);
        } catch (IOException ex)
        {
          Log.diagnostic ("Failed to cleanly disconnect", this, ex);
        } finally
        {
          connected = false;
        }
      }
      
      clientSession.close ().join ();
      clientSession = null;
    }
    
    if (executor != null)
    {
      executor.shutdown ();
      executor = null;
    }
  }
  
  public Subscription subscribe (String subscriptionExpr)
    throws IOException
  {
    Subscription sub = new Subscription (subscriptionExpr);
    
    subscribe (sub);
    
    return sub;
  }

  public void subscribe (Subscription subscription)
    throws IOException
  {
    SubAddRqst subAddRqst = new SubAddRqst (subscription.subscriptionExpr);
    subAddRqst.acceptInsecure = subscription.acceptInsecure;
    subAddRqst.keys = subscription.keys;
    
    subscription.id =
      sendAndReceive (subAddRqst, SubRply.class).subscriptionId;
    
    subscriptions.put (subscription.id, subscription);
  }
  
  public void send (Notification notification)
    throws IOException
  {
    send (new NotifyEmit (notification));
  }

  private <E extends Message> E sendAndReceive (XidMessage request,
                                                Class<E> expectedReplyType)
    throws IOException
  {
    send (request);
   
    return receive (expectedReplyType, request);
  }
  
  private synchronized void send (Message message)
    throws IOException
  {
    checkConnected ();
    clientSession.write (message);
  }
  
//  private Message receive ()
//    throws InterruptedException, NoConnectionException, IOException
//  {
//    return receive (RECEIVE_TIMEOUT);
//  }
  
  private synchronized Message receive (long timeout)
    throws IOException
  {
    try
    {
      if (lastReceived == null)
        wait (timeout);
    } catch (InterruptedException ex)
    {
      throw new RuntimeException (ex);
    }
  
    if (lastReceived != null)
    {
      Message message = lastReceived;
      
      lastReceived = null;
      
      return message;
    } else
    {
      // may have failed because we are simply not connected any more
      checkConnected ();
      
      throw new IOException ("Timeout error: did not receive a reply");
    }
  }
  
  private void checkConnected ()
    throws IllegalStateException
  {
    if (!clientSession.isConnected ())
      throw new IllegalStateException ("Not connected");
  }
  
//  private <E extends Message> E receive (Class<E> expectedMessageType)
//    throws IOException
//  {
//    return receive (null, expectedMessageType, RECEIVE_TIMEOUT);
//  }
  
  private <E extends Message> E receive (Class<E> expectedMessageType,
                                         XidMessage inResponseTo)
    throws IOException
  {
    return receive (expectedMessageType, inResponseTo, RECEIVE_TIMEOUT);
  }
  
  @SuppressWarnings("unchecked")
  private synchronized <E extends Message>
    E receive (Class<E> expectedMessageType,
               XidMessage inResponseTo, long timeout)
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
        ("Received a " + message.getClass ().getName () +
         ": was expecting " + expectedMessageType.getName ());
    }
  }
  
  protected void handleRouterMessage (Message message)
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

  private void handleDisconnect (Disconn disconn)
  {
    // todo
  }
  
  private void handleNotifyDeliver (NotifyDeliver message)
  {
    Notification ntfn = new Notification (message.attributes);
    
    fireNotify (message.secureMatches, true, ntfn);
    fireNotify (message.insecureMatches, false, ntfn);
  }

  private void fireNotify (long [] subscriptionIds,
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

  protected synchronized void handleReply (Message message)
  {
    lastReceived = message;
    
    notifyAll ();
  }
  
  class MessageHandler extends IoHandlerAdapter
  {
    public void exceptionCaught (IoSession session, Throwable cause)
      throws Exception
    {
      alarm ("Unexpected exception", this, cause);
    }

    public void messageReceived (IoSession session, Object message)
      throws Exception
    {
      if (message instanceof XidMessage)
        handleReply ((Message)message);
      else
        handleRouterMessage ((Message)message);
    }
    
    @Override
    public void sessionClosed (IoSession session)
      throws Exception
    {
      close ();
    }
  }
}
