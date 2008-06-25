package org.avis.io;

import java.io.IOException;

import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

/**
 * Test setup that creates a connected SocketAcceptor/SocketConnector
 * pair.
 * 
 * @author Matthew Phillips
 */
public class AcceptorConnectorSetup
{
  public SocketAcceptor acceptor;
  public IoSession session;
  public SocketConnector connector;

  public AcceptorConnectorSetup ()
    throws IOException
  {
    // acceptor
    acceptor = new NioSocketAcceptor (1);
    
    acceptor.setReuseAddress (true);
    
    DefaultIoFilterChainBuilder filterChainBuilder = acceptor.getFilterChain ();

    filterChainBuilder.addLast ("codec", ClientFrameCodec.FILTER);
    
    // connector
    connector = new NioSocketConnector (1);
    
    connector.setConnectTimeout (20);

    connector.getFilterChain ().addLast ("codec", ClientFrameCodec.FILTER);
  }
  
  public void connect (IoHandler acceptorListener, IoHandler connectorListener)
    throws Exception
  {
    InetSocketAddress remoteAddress = 
      new InetSocketAddress ("127.0.0.1", 29170);
    
    acceptor.setHandler (acceptorListener);
    acceptor.bind (remoteAddress);
    
    connector.setHandler (connectorListener);
    ConnectFuture future = connector.connect (remoteAddress);
    
    future.await ();
    
    session = future.getSession ();
  }
  
  
  public void close ()
  {
    session.close ();
    acceptor.dispose ();
    connector.dispose ();
  }
}
