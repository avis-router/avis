package org.avis.management.web;

import java.util.Set;

import java.io.IOException;

import java.net.InetSocketAddress;

import org.apache.asyncweb.server.ServiceContainer;
import org.apache.asyncweb.server.Transport;
import org.apache.asyncweb.server.TransportException;
import org.apache.asyncweb.server.transport.mina.DefaultHttpIoHandler;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.transport.socket.SocketAcceptor;

import org.avis.router.IoManager;

/**
 * Custom Asyncweb transport to allow web server to reuse router's IO
 * processor and thread pool.
 * 
 * @author Matthew Phillips
 */
public class AvisMinaTransport implements Transport
{
  private SocketAcceptor acceptor;
  private ServiceContainer container;
  private Set<InetSocketAddress> addresses;

  public AvisMinaTransport (Set<InetSocketAddress> addresses,
                            IoManager ioManager)
  {
    this.addresses = addresses;
    this.acceptor = ioManager.createAcceptor ();
    
    DefaultIoFilterChainBuilder chain = acceptor.getFilterChain ();

    chain.addFirst ("threadPool", ioManager.createThreadPoolFilter ());
    
    // TODO make this configurable?
    acceptor.setBacklog (100);
  }

  public void start ()
    throws TransportException
  {
    DefaultHttpIoHandler ioHandler = new DefaultHttpIoHandler ();

    ioHandler.setContainer (container);
    
    acceptor.setHandler (ioHandler);
    
    boolean success = false;
    
    try
    {
      acceptor.bind (addresses);
      
      success = true;
    } catch (IOException ex)
    {
      throw new TransportException 
        ("NIOTransport Failed to bind to address " + addresses, ex);
    } finally
    {
      if (!success)
      {
        acceptor.dispose ();
        acceptor = null;
      }
    }
  }

  /**
   * Stops this transport
   */
  public void stop () throws TransportException
  {
    if (acceptor == null)
      return;

    acceptor.dispose ();
    acceptor = null;
  }

  public void setServiceContainer (ServiceContainer container)
  {
    this.container = container;
  }
}
