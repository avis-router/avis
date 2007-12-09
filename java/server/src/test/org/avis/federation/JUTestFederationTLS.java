package org.avis.federation;

import java.util.concurrent.TimeUnit;

import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.federation.io.FederationFrameCodec;
import org.avis.federation.io.messages.FedConnRqst;
import org.avis.io.AvisX509TrustManager;
import org.avis.io.TestingIoHandler;
import org.avis.router.Router;
import org.avis.router.RouterOptionSet;
import org.avis.router.RouterOptions;

import org.junit.Test;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.Assert.fail;

import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;
import static org.avis.federation.JUTestFederation.PORT1;
import static org.avis.util.Collections.set;

public class JUTestFederationTLS
{
  @Test
  public void listener () 
    throws Exception
  {
    FederationClass fedClass = StandardFederatorSetup.defaultClass ();
    
    FederationClasses classes = new FederationClasses ();
    
    classes.mapServerDomain ("server2", fedClass);
    
    RouterOptionSet routerOptionSet = new RouterOptionSet ();
    
    routerOptionSet.inheritFrom (FederationOptionSet.OPTION_SET);
    
    RouterOptions options = new RouterOptions (routerOptionSet);
    
    options.set ("TLS.Keystore", getClass ().getResource ("router.ks"));
    options.set ("TLS.Keystore-Passphrase", "testing");
    
    Router router = new Router (options);

    EwafURI ewafURI = new EwafURI ("ewaf:/secure/127.0.0.1:" + (PORT1 + 1));
    
    Acceptor acceptor = 
      new Acceptor (router, "server1", classes, set (ewafURI), options);
    
    // connector
    TestingIoHandler listener = new TestingIoHandler ();
    IoSession session = connectFederationTLS (router, ewafURI, listener);
    
    session.write (new FedConnRqst (VERSION_MAJOR, VERSION_MINOR, "server2"));
    
    waitForSingleLink (acceptor);
    
    session.close ();
    
    acceptor.close ();
    
    router.close ();
  }
  
  private static void waitForSingleLink (final Acceptor acceptor) 
    throws InterruptedException
  {
    waitFor (8, SECONDS, new Predicate ()
    {
      public boolean satisfied ()
      {
        return acceptor.links.size () == 1;
      }
    });
  }

  private static void waitFor (long duration, TimeUnit unit,
                               Predicate predicate) 
    throws InterruptedException
  {
    long finishAt = currentTimeMillis () + unit.toMillis (duration);
    
    while (!predicate.satisfied () && currentTimeMillis () < finishAt)
      sleep (10);
    
    if (!predicate.satisfied ())
      fail ();
  }

  /**
   * Create a connection to federation listener.
   */
  private static IoSession connectFederationTLS (Router router,
                                                 EwafURI uri,
                                                 IoHandler listener)
    throws Exception
  {
    SocketConnector connector = new SocketConnector (1, router.executor ());
    SocketConnectorConfig connectorConfig = new SocketConnectorConfig ();
    
    connector.setWorkerTimeout (0);
    
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout (20);
    
    AvisX509TrustManager trustManager = 
      new AvisX509TrustManager (null, false, false);
    
    SSLContext sslContext = SSLContext.getInstance ("TLS");

    sslContext.init (null, new TrustManager [] {trustManager}, null);

    SSLFilter filter = new SSLFilter (sslContext);
    
    filter.setUseClientMode (true);
    
    connectorConfig.getFilterChain ().addFirst ("ssl", filter);   
    
    connectorConfig.getFilterChain ().addLast   
      ("codec", FederationFrameCodec.FILTER);
    
    ConnectFuture future = 
      connector.connect (new InetSocketAddress (uri.host, uri.port),
                         listener, connectorConfig);
    
    future.join ();
    
    return future.getSession ();
  }
}
