package org.avis.io;

import java.io.Closeable;
import java.io.IOException;

import java.net.InetSocketAddress;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
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
public class AcceptorConnectorSetup implements Closeable
{
  public SocketAcceptor acceptor;
  public SocketConnector connector;
  public IoSession connectorSession;

  public AcceptorConnectorSetup ()
    throws IOException
  {
    // acceptors
    acceptor = new NioSocketAcceptor ();
    
    acceptor.setReuseAddress (true);
    acceptor.getFilterChain ().addLast ("codec", ClientFrameCodec.FILTER);
    
    // connector
    
    connector = new NioSocketConnector ();
     
    connector.setConnectTimeoutMillis (5 * 1000);
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
    
    future.await (5000);
    
    connectorSession = future.getSession ();
  }
    
  public void close ()
  {
    connectorSession.close ();
    acceptor.dispose ();
    connector.dispose ();
  }
}
