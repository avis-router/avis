package org.avis.federation;

import java.util.concurrent.ExecutorService;

import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.federation.messages.FedConnRqst;
import org.avis.io.messages.Message;
import org.avis.io.messages.RequestMessage;

import org.junit.Test;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class JUTestRequestTracker
{
  /**
   * Send a message with reply, check that RequestTracker generates a
   * timeout message.
   */
  @Test
  public void timeout () 
    throws Exception
  {
    ExecutorService executor = newCachedThreadPool ();
    InetSocketAddress remoteAddress = new InetSocketAddress ("0.0.0.0", 2917);
    
    // listener
    SocketAcceptor acceptor = new SocketAcceptor (1, executor);
    SocketAcceptorConfig acceptorConfig = new SocketAcceptorConfig ();
    
    acceptorConfig.setReuseAddress (true);
    acceptorConfig.setThreadModel (ThreadModel.MANUAL);
    
    DefaultIoFilterChainBuilder filterChainBuilder =
      acceptorConfig.getFilterChain ();

    filterChainBuilder.addLast
      ("codec", new ProtocolCodecFilter (FederationFrameCodec.INSTANCE));
    
    acceptor.bind (remoteAddress, new AcceptorListener (), acceptorConfig);
    
    // connector
    SocketConnector connector = new SocketConnector (1, executor);
    SocketConnectorConfig connectorConfig = new SocketConnectorConfig ();
    
    connector.setWorkerTimeout (0);
    
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout (20);
    
    connectorConfig.getFilterChain ().addLast
      ("codec", new ProtocolCodecFilter (FederationFrameCodec.INSTANCE));
    
    ConnectorListener listener = new ConnectorListener ();
    
    ConnectFuture future = 
      connector.connect (remoteAddress, listener, connectorConfig);
    
    future.join ();
    
    IoSession session = future.getSession ();
    
    RequestTracker tracker = new RequestTracker (session);
    listener.tracker = tracker;
    
    tracker.setTimeout (2);
    
    // send message, wait for timeout message
    listener.message = null;
    
    FedConnRqst fedConnRqst = new FedConnRqst (1, 0, "test");
    
    session.write (fedConnRqst);
    
    synchronized (listener)
    {
      listener.wait (3000);
    }

    assertNotNull (listener.message);
    assertEquals (RequestTracker.TimeoutMessage.ID, listener.message.typeId ());
    assertSame (fedConnRqst, ((RequestTracker.TimeoutMessage)listener.message).request);
    
    assertNull (tracker.remove (fedConnRqst));
    
    // try again, with two in the pipe
    listener.message = null;
    
    fedConnRqst = new FedConnRqst (1, 0, "test");
    
    session.write (fedConnRqst);
    
    Thread.sleep (1000);
    
    session.write (new FedConnRqst (1, 0, "test"));
    
    synchronized (listener)
    {
      listener.wait (3000);
    }

    assertNotNull (listener.message);
    assertSame (fedConnRqst, ((RequestTracker.TimeoutMessage)listener.message).request);
    
    session.close ();
    acceptor.unbindAll ();
    executor.shutdown ();
  }
  
  static class AcceptorListener extends IoHandlerAdapter implements IoHandler
  {
    // zip
  }
  
  static class ConnectorListener extends IoHandlerAdapter implements IoHandler
  {
    public Message message;
    
    public RequestTracker tracker;

    @Override
    public void messageSent (IoSession session, Object theMessage)
      throws Exception
    {
      if (theMessage instanceof RequestMessage<?>)
        tracker.add ((RequestMessage<?>)theMessage);
    }
    
    @Override
    public synchronized void messageReceived (IoSession session, 
                                              Object theMessage)
      throws Exception
    {
      message = (Message)theMessage;
      
      notifyAll ();
    }
  }
}
