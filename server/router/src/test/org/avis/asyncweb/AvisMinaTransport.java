package org.avis.asyncweb;

import java.util.concurrent.ExecutorService;

import java.io.IOException;

import java.net.InetSocketAddress;

import org.apache.asyncweb.server.ServiceContainer;
import org.apache.asyncweb.server.Transport;
import org.apache.asyncweb.server.TransportException;
import org.apache.asyncweb.server.transport.mina.DefaultHttpIoHandler;
import org.apache.asyncweb.server.transport.mina.HttpIoHandler;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

public class AvisMinaTransport implements Transport
{
  private static final int DEFAULT_PORT = 9012;

  private static final int DEFAULT_IO_THREADS = Runtime.getRuntime ()
      .availableProcessors ();

  private static final int DEFAULT_EVENT_THREADS = 16;

  private SocketAcceptor acceptor;

  private ExecutorService eventExecutor;

  private int port = DEFAULT_PORT;

  private String address;

  private int ioThreads = DEFAULT_IO_THREADS;

  private int eventThreads = DEFAULT_EVENT_THREADS;

  private HttpIoHandler ioHandler;

  private ServiceContainer container;

  /**
   * Sets the port this transport will listen on
   * 
   * @param port The port
   */
  public void setPort (int port)
  {
    this.port = port;
  }

  /**
   * Sets the address this transport will listen on
   * 
   * @param address The address to bind to. Specify <tt>null</tt> or
   *                <tt>"*"</tt> to listen to all NICs (Network
   *                Interface Cards).
   */
  public void setAddress (String address)
  {
    if ("*".equals (address))
    {
      address = null;
    }
    this.address = address;
  }

  public int getIoThreads ()
  {
    return ioThreads;
  }

  /**
   * Sets the number of worker threads employed by this transport.
   * This should typically be a small number (2 is a good choice) -
   * and is not tied to the number of concurrent connections you wish
   * to support
   * 
   * @param ioThreads The number of worker threads to employ
   */
  public void setIoThreads (int ioThreads)
  {
    this.ioThreads = ioThreads;
  }

  public int getEventThreads ()
  {
    return eventThreads;
  }

  public void setEventThreads (int eventThreads)
  {
    this.eventThreads = eventThreads;
  }

  /**
   * Sets the <code>ServiceContainer</code> to which we issue
   * requests
   * 
   * @param container Our associated <code>ServiceContainer</code>
   */
  public void setServiceContainer (ServiceContainer container)
  {
    this.container = container;
  }

  /**
   * Sets the <code>HttpIOHandler</code> to be employed by this
   * transport
   * 
   * @param httpIOHandler The handler to be employed by this transport
   */
  public void setIoHandler (HttpIoHandler httpIOHandler)
  {
    this.ioHandler = httpIOHandler;
  }

  /**
   * Starts this transport
   * 
   * @throws TransportException If the transport can not be started
   */
  public void start () throws TransportException
  {
    initIOHandler ();
    acceptor = new NioSocketAcceptor (ioThreads);
    eventExecutor = new OrderedThreadPoolExecutor (this.eventThreads);

    boolean success = false;
    try
    {
      DefaultIoFilterChainBuilder chain = acceptor.getFilterChain ();

      chain.addFirst ("threadPool",
                      new ExecutorFilter (eventExecutor));
      acceptor.setReuseAddress (true);
      acceptor.getSessionConfig ().setReuseAddress (true);

//      chain.addLast ("mdc", new MdcInjectionFilter ());
//
//      if (isLoggingTraffic)
//      {
//        LOG.debug ("Configuring traffic logging filter");
//        LoggingFilter filter = new LoggingFilter ();
//        filter.setLogLevel (IoEventType.CLOSE, logLevel);
//        filter.setLogLevel (IoEventType.EXCEPTION_CAUGHT, logLevel);
//        filter.setLogLevel (IoEventType.MESSAGE_RECEIVED, logLevel);
//        filter.setLogLevel (IoEventType.MESSAGE_SENT, logLevel);
//        filter.setLogLevel (IoEventType.SESSION_CLOSED, logLevel);
//        filter.setLogLevel (IoEventType.SESSION_CREATED, logLevel);
//        filter.setLogLevel (IoEventType.SESSION_IDLE, logLevel);
//        filter.setLogLevel (IoEventType.SESSION_OPENED, logLevel);
//        filter.setLogLevel (IoEventType.SET_TRAFFIC_MASK, logLevel);
//        filter.setLogLevel (IoEventType.WRITE, logLevel);
//        acceptor.getFilterChain ().addLast ("logging", filter);
//      }

      // TODO make this configurable instead of hardcoding like this
      acceptor.setBacklog (100);
      acceptor.setHandler (ioHandler);

      if (address != null)
      {
        acceptor.bind (new InetSocketAddress (address, port));
      } else
      {
        acceptor.bind (new InetSocketAddress (port));
      }

      success = true;
    } catch (IOException e)
    {
      throw new TransportException (
          "NIOTransport Failed to bind to port " + port, e);
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
    {
      return;
    }

    acceptor.dispose ();
    eventExecutor.shutdown ();
    acceptor = null;
    eventExecutor = null;
  }

  /**
   * @return A string representation of this transport
   */
  @Override
  public String toString ()
  {
    return "NIOTransport [port=" + port + "]";
  }

  /**
   * Initializes our handler - creating a new (default) handler if
   * none has been specified
   * 
   * @throws IllegalStateException If we have not yet been associated
   *                 with a container
   */
  private void initIOHandler ()
  {
    if (ioHandler == null)
    {
      ioHandler = new DefaultHttpIoHandler ();
    }

    if (container == null)
    {
      throw new IllegalStateException (
          "Transport not associated with a container");
    }

    ioHandler.setContainer (container);
  }
}
