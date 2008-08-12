package org.avis.common;

import java.net.InetSocketAddress;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.junit.Test;

import static java.lang.System.currentTimeMillis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test MINA's session close callback. Written to test suspected MINA
 * 2.0 bug: can remove when sure its fixed.
 */
public class JUTestSessionClose
{
  private static final int TIMEOUT = 5000;

  @Test
  public void sessionClose ()
    throws Exception
  {
    InetSocketAddress address = new InetSocketAddress ("127.0.0.1", 29170);

    SocketAcceptor acceptor = new NioSocketAcceptor ();
    acceptor.setReuseAddress (true);
    
    // close session as soon as it's opened
    acceptor.setHandler (new IoHandlerAdapter ()
    {
      @Override
      public void sessionOpened (IoSession session)
        throws Exception
      {
        session.close ();
      }
    });
    
    acceptor.bind (address);

    SocketConnector connector = new NioSocketConnector (1);
    
    final Object lock = new Object ();
    
    // call notify on lock when session is closed
    connector.setHandler (new IoHandlerAdapter ()
    {
      @Override
      public void sessionClosed (IoSession session)
        throws Exception
      {
        synchronized (lock)
        {
          lock.notify ();
        }
      } 
    }); 
    
    ConnectFuture connectFuture = connector.connect (address);
    
    assertTrue (connectFuture.await (TIMEOUT));
    
    long startedAt = currentTimeMillis ();

    // wait for connector to receive sessionClosed
    synchronized (lock)
    {
      lock.wait (TIMEOUT);
    }

    assertFalse ("Did not receive close",
                 connectFuture.getSession ().isConnected ());
    
    assertTrue (currentTimeMillis () - startedAt < TIMEOUT);
    
    connector.dispose ();
    acceptor.dispose ();
  }
}


