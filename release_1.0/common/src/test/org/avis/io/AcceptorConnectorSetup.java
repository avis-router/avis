package org.avis.io;

import java.util.concurrent.ExecutorService;

import java.io.IOException;

import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import static java.util.concurrent.Executors.newCachedThreadPool;

/**
 * Test setup that creates a connected SocketAcceptor/SocketConnector
 * pair.
 * 
 * @author Matthew Phillips
 */
public class AcceptorConnectorSetup
{
  public SocketAcceptor acceptor;
  public SocketAcceptorConfig acceptorConfig;
  public IoSession session;
  public ExecutorService executor;
  public SocketConnector connector;
  public SocketConnectorConfig connectorConfig;

  public AcceptorConnectorSetup ()
    throws IOException
  {
    executor = newCachedThreadPool ();
    
    // listener
    acceptor = new SocketAcceptor (1, executor);
    acceptorConfig = new SocketAcceptorConfig ();
    
    acceptorConfig.setReuseAddress (true);
    acceptorConfig.setThreadModel (ThreadModel.MANUAL);
    
    DefaultIoFilterChainBuilder filterChainBuilder =
      acceptorConfig.getFilterChain ();

    filterChainBuilder.addLast ("codec", ClientFrameCodec.FILTER);
    
    // connector
    connector = new SocketConnector (1, executor);
    connectorConfig = new SocketConnectorConfig ();
    
    connector.setWorkerTimeout (0);
    
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout (20);
    
    connectorConfig.getFilterChain ().addLast   
      ("codec", ClientFrameCodec.FILTER);
  }
  
  public void connect (IoHandler acceptorListener, IoHandler connectorListener)
    throws IOException
  {
    InetSocketAddress remoteAddress = new InetSocketAddress ("127.0.0.1", 29170);
    
    acceptor.bind (remoteAddress, acceptorListener, acceptorConfig);
    
    ConnectFuture future = 
      connector.connect (remoteAddress, connectorListener, connectorConfig);
    
    future.join ();
    
    session = future.getSession ();
  }
  
  
  public void close ()
  {
    session.close ();
    acceptor.unbindAll ();
    executor.shutdown ();
  }
}
