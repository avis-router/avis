package org.avis.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import java.io.IOException;
import java.io.InputStream;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.SimpleIoProcessorPool;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.apache.mina.filter.traffic.MessageSizeEstimator;
import org.apache.mina.filter.traffic.ReadThrottleFilter;
import org.apache.mina.filter.traffic.ReadThrottlePolicy;
import org.apache.mina.transport.socket.nio.NioProcessor;
import org.apache.mina.transport.socket.nio.NioSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import org.avis.common.ElvinURI;
import org.avis.io.FrameCodec;
import org.avis.io.Net;
import org.avis.io.messages.Message;
import org.avis.util.Filter;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.apache.mina.common.IoBuffer.setUseDirectBuffer;
import static org.avis.io.TLS.toPassphrase;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.warn;

/**
 * Handles management of network I/O, including pooling of I/O
 * processors, acceptor/connector setup, security (SSL
 * authentication), and common filter creation.
 * 
 * @author Matthew Phillips
 */
public class IoManager
{
  /**
   * Used by read throttle to estimate size of messages in buffer.
   */
  private static final MessageSizeEstimator MESSAGE_SIZE_ESTIMATOR = 
    new MessageSizeEstimator ()
  {
    public int estimateSize (Object message)
    {
      return max (0, ((Message)message).frameSize * 2);
    }
  };

  private SimpleIoProcessorPool<NioSession> processorPool;
  
  private ExecutorService ioExecutor;
  private OrderedThreadPoolExecutor filterExecutor;
  private ScheduledExecutorService throttleExecutor;
  
  private String keystorePassphrase;
  private KeyStore keystore;

  private Map<ElvinURI, Collection<NioSocketAcceptor>> uriToAcceptors;
  private Map<ElvinURI, NioSocketConnector> uriToConnectors;
  
  /**
   * Create a new instance.
   * 
   * @param keystoreUri The URI for the SSL keystore. May be null, in
   *          which case attempting to use a secure Elvin URI will
   *          fail.
   * @param keystorePassphrase The keystore passphrase.
   * @param useDirectBuffers True if MINA should use direct IO buffers.
   */
  public IoManager (URI keystoreUri, String keystorePassphrase,
                    boolean useDirectBuffers) 
    throws IOException
  {
    this.uriToAcceptors = 
      new HashMap<ElvinURI, Collection<NioSocketAcceptor>> ();
    this.uriToConnectors = new HashMap<ElvinURI, NioSocketConnector> ();
    
    this.keystorePassphrase = keystorePassphrase;
    this.ioExecutor = newCachedThreadPool ();
    this.processorPool = 
      new SimpleIoProcessorPool<NioSession> 
        (NioProcessor.class, ioExecutor, 
         getRuntime ().availableProcessors () + 1);
    this.filterExecutor = new OrderedThreadPoolExecutor (0, 16, 32, SECONDS);
    this.throttleExecutor = newScheduledThreadPool (1);
    
    if (keystoreUri != null)
      this.keystore = loadKeystore (keystoreUri);
    
    setUseDirectBuffer (useDirectBuffers);
  }

  public void close ()
  {
    for (Collection<NioSocketAcceptor> acceptors : uriToAcceptors.values ())
    {
      for (NioSocketAcceptor acceptor : acceptors)
        acceptor.dispose ();
    }
    
    for (NioSocketConnector connector : uriToConnectors.values ())
      connector.dispose ();
    
    ioExecutor.shutdown ();
    filterExecutor.shutdown ();
    throttleExecutor.shutdown ();
    
    try
    {
      if (!ioExecutor.awaitTermination (15, SECONDS))
        warn ("Failed to cleanly shut down thread pool", this);
    } catch (InterruptedException ex)
    {
      diagnostic ("Interrupted while waiting for shutdown", this, ex);
    }
  }

  public IoFilter createThreadPoolFilter ()
  {
    return new ExecutorFilter (filterExecutor);
  }

  /**
   * Create a filter that throttles incoming data after a max buffer
   * size is passed. This only works for connections using
   * {@link FrameCodec} for messages
   */
  public IoFilter createThrottleFilter (int maxBufferSize)
  {
    ReadThrottleFilter readThrottle = 
      new ReadThrottleFilter (throttleExecutor, ReadThrottlePolicy.BLOCK, 
                              MESSAGE_SIZE_ESTIMATOR);
    
    readThrottle.setMaxSessionBufferSize (maxBufferSize);
    
    return readThrottle;
  }

  public NioSocketAcceptor createAcceptor ()
  {
    NioSocketAcceptor acceptor = new NioSocketAcceptor (processorPool);

    acceptor.setReuseAddress (true);
    
    return acceptor;
  }

  /**
   * Bind to a set of URI's to network addresses.
   * 
   * @param uris The URI's to bind.
   * @param handler The IO handler.
   * @param filters The basic IO filters.
   * @param authenticationRequiredHosts Hosts matched by this filter
   *          must be successfully authenticated via TLS or will be
   *          refused access.
   * 
   * @return The created acceptors.
   * 
   * @throws IOException if an error occurred during binding.
   */
  public void bind 
    (Set<? extends ElvinURI> uris,
     IoHandler handler,
     DefaultIoFilterChainBuilder filters,
     Filter<InetAddress> authenticationRequiredHosts)
    throws IOException
  {
    DefaultIoFilterChainBuilder secureFilters = null; // lazy init

    for (ElvinURI uri : uris)
    {
      Set<InetSocketAddress> addresses = Net.addressesFor (uri);
      Collection<NioSocketAcceptor> uriAcceptors = 
        new ArrayList<NioSocketAcceptor> (addresses.size ());

      for (InetSocketAddress address : addresses)
      {
        NioSocketAcceptor acceptor = createAcceptor ();

        if (uri.isSecure ())
        {
          if (secureFilters == null)
          {
            secureFilters = 
              createSecureFilters (filters, authenticationRequiredHosts,
                                   false);
          }

          acceptor.setFilterChainBuilder (secureFilters);
        } else
        {
          acceptor.setFilterChainBuilder (filters);
        }

        acceptor.setHandler (handler);
        acceptor.bind (address);

        uriAcceptors.add (acceptor);
      }
      
      uriToAcceptors.put (uri, uriAcceptors);
    }
  }
  
  /**
   * Reverse the effect of {@link #bind()}.
   */
  public void unbind (Collection<? extends ElvinURI> uris)
  {
    for (ElvinURI uri: uris)
    {
      if (uriToAcceptors.containsKey (uri))
      {
        for (NioSocketAcceptor acceptor : uriToAcceptors.get (uri))
          acceptor.dispose ();
      }
    }
    
    for (ElvinURI uri: uris)
      uriToAcceptors.remove (uri);
  }

  /**
   * Create the filters used for TLS-secured links.
   * 
   * @param commonFilters The initial set of filters to add to.
   * @param authRequired The hosts for which authentication is
   *                required.
   * @param clientMode True if the TLS filter should be in client mode.
   * 
   * @return The new filter set.
   */
  private DefaultIoFilterChainBuilder createSecureFilters 
    (DefaultIoFilterChainBuilder commonFilters, 
     Filter<InetAddress> authRequired, 
     boolean clientMode) 
  {
    DefaultIoFilterChainBuilder secureFilters = 
      new DefaultIoFilterChainBuilder (commonFilters);
    
    secureFilters.addFirst 
      ("security", 
       new SecurityFilter (keystore, keystorePassphrase, 
                           authRequired, clientMode));
    
    return secureFilters;
  }
  
  /**
   * Create the filters used for standard, non secured links.
   * 
   * @param commonFilters The initial set of filters to add to.
   * @param authRequired The hosts for which authentication is
   *                required. For standard link these hosts are denied
   *                connection.
   * @return The new filter set.
   */
  private DefaultIoFilterChainBuilder createStandardFilters 
    (DefaultIoFilterChainBuilder commonFilters, 
     Filter<InetAddress> authRequired)
  {
    if (authRequired != Filter.MATCH_NONE)
    {
      commonFilters = new DefaultIoFilterChainBuilder (commonFilters); 
      
      commonFilters.addFirst ("blacklist", new BlacklistFilter (authRequired));
    }
    
    return commonFilters;
  }

  /**
   * Lazy load the SSL keystore.
   */
  private KeyStore loadKeystore (URI keystoreUri) 
    throws IOException
  {
    if (keystoreUri == null)
    {
      throw new IOException 
        ("Cannot use TLS without a keystore: " +
         "see TLS.Keystore configuration option");
    }
    
    InputStream keystoreStream = keystoreUri.toURL ().openStream ();

    try
    {
      KeyStore newKeystore = KeyStore.getInstance ("JKS");
      
      newKeystore.load (keystoreStream, toPassphrase (keystorePassphrase));
      
      return newKeystore;
    } catch (GeneralSecurityException ex)
    {
      throw new IOException ("Failed to load TLS keystore: " + 
                             ex.getMessage ());
    } finally
    {
      keystoreStream.close ();
    }    
  }

  /**
   * Get the addresses for all URI's bound with {@link #bind()}
   */
  public Set<InetSocketAddress> addressesFor (Collection<? extends ElvinURI> uris)
  {
    Set<InetSocketAddress> addresses = new TreeSet<InetSocketAddress> ();
    
    for (ElvinURI uri : uris)
    {
      if (uriToAcceptors.containsKey (uri))
      {
        for (NioSocketAcceptor acceptor : uriToAcceptors.get (uri))
          addresses.add (acceptor.getLocalAddress ());
      }
    }
    
    return addresses;
  }

  /**
   * Get the sessions for all URI's bound with {@link #bind()}
   */
  public Collection<IoSession> sessionsFor (Collection<? extends ElvinURI> uris)
  {
    Collection<IoSession> sessions = new ArrayList<IoSession> ();
    
    for (ElvinURI uri : uris)
    {
      if (uriToAcceptors.containsKey (uri))
      {
        for (NioSocketAcceptor acceptor : uriToAcceptors.get (uri))
          sessions.addAll (acceptor.getManagedSessions ());
      }
    }
    
    return sessions;
  }

  public NioSocketConnector createConnector ()
  {
    return new NioSocketConnector (processorPool);
  }

  /**
   * Connect to URI.
   * 
   * @param uri The URI.
   * @param handler The IO handler.
   * @param filters The base filters.
   * @param authenticationRequiredHosts The hosts for which
   *          authentication is required. For standard link these
   *          hosts are denied connection.
   * @param timeout Connect timeout (in seconds).
   * @return The connect future.
   */
  public IoFuture connect (ElvinURI uri, IoHandler handler,
                           DefaultIoFilterChainBuilder filters,
                           Filter<InetAddress> authenticationRequiredHosts, 
                           int timeout)
  {
    NioSocketConnector connector = new NioSocketConnector (processorPool);
    
    uriToConnectors.put (uri, connector);
    
    if (uri.isSecure ())
      filters = createSecureFilters (filters, authenticationRequiredHosts, true);
    else
      filters = createStandardFilters (filters, authenticationRequiredHosts);
    
    connector.setFilterChainBuilder (filters);
    connector.setHandler (handler);
    connector.setConnectTimeout (timeout);
    
    return connector.connect (new InetSocketAddress (uri.host, uri.port));
  }

  public void disconnect (ElvinURI uri)
  {
    NioSocketConnector connector = uriToConnectors.remove (uri);
    
    if (connector != null)
      connector.dispose ();
  }
}
