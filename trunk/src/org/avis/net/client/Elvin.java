package org.avis.net.client;

import java.util.HashMap;
import java.util.Map;

import java.io.IOException;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.net.FrameCodec;
import org.avis.net.messages.ConnRply;
import org.avis.net.messages.ConnRqst;
import org.avis.net.messages.Disconn;
import org.avis.net.messages.DisconnRply;
import org.avis.net.messages.DisconnRqst;
import org.avis.net.messages.Message;
import org.avis.net.messages.Nack;
import org.avis.net.messages.NotifyDeliver;
import org.avis.net.messages.SubAddRqst;
import org.avis.net.messages.SubRply;
import org.avis.net.messages.XidMessage;

import dsto.dfc.logging.Log;

import static dsto.dfc.logging.Log.alarm;

import static org.avis.Common.CLIENT_VERSION_MAJOR;
import static org.avis.Common.CLIENT_VERSION_MINOR;
import static org.avis.net.client.ElvinURI.defaultProtocol;

public class Elvin
{
  private static final int RECEIVE_TIMEOUT = 5000;
  
  private ElvinURI elvinUri;
  private IoSession clientSession;
  private boolean connected;
  private Map<Long, Subscription> subscriptions;
  
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
    
    ConnRqst connRqst =
      new ConnRqst (CLIENT_VERSION_MAJOR, CLIENT_VERSION_MINOR);
    
    send (connRqst);
    
    receive (connRqst, ConnRply.class);
    
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
      
      SocketConnectorConfig cfg = new SocketConnectorConfig ();
      cfg.setConnectTimeout (10);
      
      DemuxingProtocolCodecFactory codecFactory =
        new DemuxingProtocolCodecFactory ();
      codecFactory.register (FrameCodec.class);
      
      cfg.getFilterChain ().addLast
        ("codec", new ProtocolCodecFilter (codecFactory));
      
      ConnectFuture future =
        connector.connect
          (new InetSocketAddress (elvinUri.host, elvinUri.port),
           new MessageHandler (), cfg);
                                       
      future.join ();
      clientSession = future.getSession ();
    } catch (RuntimeIOException ex)
    {
      // unwrap MINA's RuntimeIOException
      throw (IOException)ex.getCause ();
    }
  }
  
  public void close ()
  {
    if (connected)
    {
      try
      {
        DisconnRqst disconnRqst = new DisconnRqst ();
        send (disconnRqst);
        receive (disconnRqst, DisconnRply.class);
      } catch (IOException ex)
      {
        Log.diagnostic ("Failed to cleanly disconnect", this, ex);
      }
    }
    
    clientSession.close ().join ();
    clientSession = null;
    connected = false;
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
    
    send (subAddRqst);
    
    SubRply reply = receive (subAddRqst, SubRply.class);
    
    subscription.id = reply.subscriptionId;
    
    subscriptions.put (subscription.id, subscription);
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
  
//  private <E extends Message> E receive (Class<E> type)
//    throws IOException
//  {
//    return receive (null, type, RECEIVE_TIMEOUT);
//  }
  
  private <E extends Message> E receive (XidMessage request,
                                         Class<E> type)
    throws IOException
  {
    return receive (request, type, RECEIVE_TIMEOUT);
  }
  
//  private synchronized <E extends Message> E receive (Class<E> type, long timeout)
//    throws IOException
//  {
//    return receive (null, type, timeout);
//  }
  
  @SuppressWarnings("unchecked")
  private synchronized <E extends Message> E receive (XidMessage request,
                                                      Class<E> type, long timeout)
    throws IOException
  {
    Message message = receive (timeout);
      
    if (type.isAssignableFrom (message.getClass ()))
    {
      if (request != null && request.xid != ((XidMessage)message).xid)
        throw new IllegalStateException ("XID mismatch");
      
      return (E)message;
    } else if (message instanceof Nack)
    {
      throw new IOException
        ("Router rejected request: " + ((Nack)message).message);
    } else
    {
      throw new IllegalStateException
        ("Received a " + message.getClass ().getName () +
         ": was expecting " + type.getName ());
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
    Notification ntfn = new Notification ();
    ntfn.attributes = message.attributes;
    
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
      NotificationEvent event = new NotificationEvent (this, subscription, ntfn, secure);
      
      subscription.notifyListeners (event);
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
  }
}
