package org.avis.federation;

import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.federation.io.FederationFrameCodec;
import org.avis.federation.io.messages.FedConnRqst;
import org.avis.io.TestingIoHandler;
import org.avis.router.Router;
import org.avis.router.RouterOptionSet;
import org.avis.router.RouterOptions;

import org.junit.Test;

import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;
import static org.avis.federation.JUTestFederation.PORT1;
import static org.avis.federation.JUTestFederation.PORT2;
import static org.avis.federation.TestUtils.waitForConnect;
import static org.avis.federation.TestUtils.waitForSingleLink;
import static org.avis.io.TLS.defaultSSLContext;
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
  
  /**
   * Full connect/accept test over TLS.
   */
  @Test
  public void full () 
    throws Exception
  {
    FederationClass fedClass = StandardFederatorSetup.defaultClass ();
    EwafURI ewafURI = new EwafURI ("ewaf:/secure/127.0.0.1:" + (PORT1 + 1));
   
    // router1 (listener)
    FederationClasses classes = new FederationClasses ();
    
    classes.mapServerDomain ("server2", fedClass);
    
    RouterOptionSet routerOptionSet = new RouterOptionSet ();
    
    routerOptionSet.inheritFrom (FederationOptionSet.OPTION_SET);
    
    RouterOptions options1 = new RouterOptions (routerOptionSet);
    
    options1.set ("Listen", "elvin://127.0.0.1:" + PORT1);
    options1.set ("TLS.Keystore", getClass ().getResource ("router.ks"));
    options1.set ("TLS.Keystore-Passphrase", "testing");
    
    Router router1 = new Router (options1);

    Acceptor acceptor = 
      new Acceptor (router1, "server1", classes, set (ewafURI), options1);
    
    // router 2 (connector)
    RouterOptions options2 = new RouterOptions (routerOptionSet);
    
    options2.set ("Listen", "elvin://127.0.0.1:" + PORT2);
    options2.set ("TLS.Keystore", getClass ().getResource ("router.ks"));
    options2.set ("TLS.Keystore-Passphrase", "testing");
    
    Router router2 = new Router (options2);

    Connector connector = 
      new Connector (router2, "server2", ewafURI, fedClass, options2);
    
    waitForConnect (connector);
    waitForSingleLink (acceptor);
    
    connector.close ();
    acceptor.close ();
    
    router1.close ();
    router2.close ();
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
    
    SSLFilter filter = new SSLFilter (defaultSSLContext ());
    
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
