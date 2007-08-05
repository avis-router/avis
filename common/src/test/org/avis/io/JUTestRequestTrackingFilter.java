package org.avis.io;

import java.util.concurrent.ExecutorService;

import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.Message;
import org.avis.io.messages.RequestTimeoutMessage;

import org.junit.Test;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JUTestRequestTrackingFilter
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

    filterChainBuilder.addLast ("codec", ClientFrameCodec.FILTER);
    
    acceptor.bind (remoteAddress, new AcceptorListener (), acceptorConfig);
    
    // connector
    SocketConnector connector = new SocketConnector (1, executor);
    SocketConnectorConfig connectorConfig = new SocketConnectorConfig ();
    
    connector.setWorkerTimeout (0);
    
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout (20);
    
    connectorConfig.getFilterChain ().addLast   
      ("codec", ClientFrameCodec.FILTER);
    
    RequestTrackingFilter requestTrackingFilter = new RequestTrackingFilter (2);
    
    connectorConfig.getFilterChain ().addLast 
      ("tracker", requestTrackingFilter);
    
    ConnectorListener listener = new ConnectorListener ();
    
    ConnectFuture future = 
      connector.connect (remoteAddress, listener, connectorConfig);
    
    future.join ();
    
    IoSession session = future.getSession ();
    
    // send message, wait for timeout message
    listener.message = null;
    
    ConnRqst fedConnRqst = new ConnRqst (1, 0);
    
    session.write (fedConnRqst);
    
    synchronized (listener)
    {
      listener.wait (3000);
    }

    assertNotNull (listener.message);
    assertEquals (RequestTimeoutMessage.ID, listener.message.typeId ());
    assertSame (fedConnRqst, ((RequestTimeoutMessage)listener.message).request);
    
    // try again, with two in the pipe
    listener.message = null;
    
    fedConnRqst = new ConnRqst (1, 0);
    
    session.write (fedConnRqst);
    
    Thread.sleep (1000);
    
    session.write (new ConnRqst (1, 0));
    
    synchronized (listener)
    {
      listener.wait (3000);
    }

    assertNotNull (listener.message);
    assertSame (fedConnRqst, ((RequestTimeoutMessage)listener.message).request);
    
    session.close ();
    acceptor.unbindAll ();
    executor.shutdown ();
    
    assertTrue (requestTrackingFilter.sharedResourcesDisposed ());
  }
  
  static class AcceptorListener extends IoHandlerAdapter implements IoHandler
  {
    // zip
  }
  
  static class ConnectorListener extends IoHandlerAdapter implements IoHandler
  {
    public Message message;

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
