package org.avis.federation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.avis.config.Options;
import org.avis.federation.io.FederationFrameCodec;
import org.avis.federation.io.messages.FedConnRqst;
import org.avis.federation.io.messages.FedSubReplace;
import org.avis.io.TestingIoHandler;
import org.avis.io.messages.Nack;
import org.avis.io.messages.NotifyDeliver;
import org.avis.io.messages.NotifyEmit;
import org.avis.io.messages.SecRqst;
import org.avis.router.Router;
import org.avis.router.SimpleClient;
import org.avis.security.Key;
import org.avis.security.Keys;
import org.avis.subscription.ast.nodes.Const;
import org.avis.util.LogFailTester;

import static java.lang.Thread.sleep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import static org.avis.federation.Federation.DEFAULT_EWAF_PORT;
import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;
import static org.avis.federation.TestUtils.waitForAsyncConnect;
import static org.avis.federation.TestUtils.waitForConnect;
import static org.avis.logging.Log.INFO;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.enableLogging;
import static org.avis.logging.Log.shouldLog;
import static org.avis.security.DualKeyScheme.Subset.CONSUMER;
import static org.avis.security.DualKeyScheme.Subset.PRODUCER;
import static org.avis.security.KeyScheme.SHA1_DUAL;
import static org.avis.security.KeyScheme.SHA1_PRODUCER;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.util.Collections.set;

public class JUTestFederation
{
  static final int PORT1 = 29170;
  static final int PORT2 = 29180;
  
  private static final String MANTARA_ELVIN = "/usr/local/sbin/elvind";
  
  private LogFailTester logTester;
  private StandardFederatorSetup federation;
  private boolean oldLogInfoState;
  private List<Closeable> autoClose;
  
  @Before
  public void setup ()
  {
    // enableLogging (Log.TRACE, true);
    // enableLogging (Log.DIAGNOSTIC, true);

    autoClose = new ArrayList<Closeable> ();
    
    oldLogInfoState = shouldLog (INFO);
    
    enableLogging (INFO, false);
    
    logTester = new LogFailTester ();
  }
  
  @After
  public void tearDown ()
    throws Exception
  {
    enableLogging (INFO, oldLogInfoState);
    
    if (federation != null)
    {
      federation.close ();
      federation = null;
    }
    
    // close in reverse order to add
    for (int i = autoClose.size () - 1; i >= 0; i--)
    {
      try
      {
        autoClose.get (i).close ();
      } catch (Throwable ex)
      {
        alarm ("Failed to close", this, ex);
      }
    }

    logTester.assertOkAndDispose ();
  }
  
  @Test
  public void uri () 
    throws Exception
  {
    EwafURI uri = new EwafURI ("ewaf://hostname");
    
    assertEquals ("ewaf", uri.scheme);
    assertEquals ("hostname", uri.host);
    assertEquals (VERSION_MAJOR, uri.versionMajor);
    assertEquals (VERSION_MINOR, uri.versionMinor);
    assertEquals (DEFAULT_EWAF_PORT, uri.port);
  }
  
  @Test
  public void wildcards () 
    throws Exception
  {
    FederationClasses classes = new FederationClasses ();
    
    FederationClass class1 = classes.define ("class1");
    FederationClass class2 = classes.define ("class2");
    
    classes.map ("*.elvin.org", class1);
    classes.map ("111.111.111.1??", class2);
    
    assertSame (class1, classes.classFor ("public.elvin.org"));
    assertSame (class2, classes.classFor ("111.111.111.123"));
    assertSame (class2, classes.classFor ("111.111.111.134"));
    assertSame (classes.defaultClass (), classes.classFor ("111.111.111.234"));
  }
  
  @Test
  public void basic ()
    throws Exception
  {
    federation = new StandardFederatorSetup ();
    
    testTwoWayClientSendReceive (federation.client1, federation.client2);
  }
  
  /**
   * Check that case does not matter for hostnames.
   */
  @Test
  public void hostnameCaseInsensitive ()
    throws Exception
  {
    FederationClass fedClass = StandardFederatorSetup.defaultClass ();
    
    FederationClasses classes1 = new FederationClasses ();
    FederationClasses classes2 = new FederationClasses ();
    
    classes1.map ("LOCALhost", fedClass);
    classes2.map ("localHOSt", fedClass);
    
    federation = new StandardFederatorSetup (classes1, classes2);
  }
  
  /**
   * Test the acceptor rejects various invalid attempts to connect.
   */
  @Test
  public void rejection ()
    throws Exception
  {
    FederationClass fedClass2 = StandardFederatorSetup.defaultClass ();
    
    FederationClasses classes = new FederationClasses ();
    
    classes.map ("localhost", fedClass2);
    
    Router server1 = new Router (PORT1);
    
    autoClose (server1);
    
    EwafURI ewafURI = new EwafURI ("ewaf://localhost:" + (PORT1 + 1));
    Options options = new Options (FederationOptionSet.OPTION_SET);
    
    Acceptor acceptor = 
      new Acceptor (server1, "server1", classes, set (ewafURI), options);

    autoClose (acceptor);
    
    // connector
    TestingIoHandler listener = new TestingIoHandler ();
    IoSession connectSession = connectFederation (ewafURI, listener);
    
    // incompatible version
    logTester.pause ();
    
    connectSession.write
      (new FedConnRqst (Federation.VERSION_MAJOR + 1, 0, "server2"));
    
    Nack nack = listener.waitForMessage (Nack.class);
    
    logTester.unpause ();
    
    assertEquals (Nack.PROT_INCOMPAT, nack.error);
    
    connectSession.close ();
    
    // bad server domain (same as acceptor's)
    listener = new TestingIoHandler ();
    connectSession = connectFederation (ewafURI, listener);
    
    logTester.pause ();
    
    connectSession.write
      (new FedConnRqst (Federation.VERSION_MAJOR, 
                        Federation.VERSION_MINOR, "server1"));
    
    nack = listener.waitForMessage (Nack.class);
    
    logTester.unpause ();
        
    assertEquals (Acceptor.INVALID_DOMAIN, nack.error);
    
    connectSession.close ();
    
    // no federation class mapped
    
    classes.clear ();
    
    listener = new TestingIoHandler ();
    connectSession = connectFederation (ewafURI, listener);
    
    logTester.pause ();
    
    connectSession.write
      (new FedConnRqst (Federation.VERSION_MAJOR, 
                        Federation.VERSION_MINOR, "bogus"));
    
    nack = listener.waitForMessage (Nack.class);
    
    logTester.unpause ();
        
    assertEquals (Acceptor.INVALID_DOMAIN, nack.error);
    
    connectSession.close ();
    connectSession.getService ().dispose ();
    
    classes.map ("localhost", fedClass2);
    
    // bogus handshake
    listener = new TestingIoHandler ();
    connectSession = connectFederation (ewafURI, listener);
    
    logTester.pause ();
    
    connectSession.write (new FedSubReplace (Const.CONST_FALSE));
    
    // acceptor should just abort connection
    listener.waitForClose (connectSession);
    
    logTester.unpause ();
  }
  
  @Test
  public void addAttributes ()
    throws Exception
  {
    FederationClass fedClass1 = StandardFederatorSetup.defaultClass ();
        
    fedClass1.incomingAttributes = new HashMap<String, Object> ();
    fedClass1.outgoingAttributes = new HashMap<String, Object> ();
    
    fedClass1.incomingAttributes.put ("Incoming", "incoming");
    fedClass1.outgoingAttributes.put ("Outgoing", "outgoing");
    
    FederationClasses classes1 = 
      new FederationClasses (fedClass1);
    FederationClasses classes2 = 
      new FederationClasses (StandardFederatorSetup.defaultClass ());
    
    federation = new StandardFederatorSetup (classes1, classes2);
    
    // client 1 -> client 2
    federation.client1.sendNotify 
      (map ("federated", "server1", "from", "client1"));
    
    NotifyDeliver notification = (NotifyDeliver)federation.client2.receive ();
    
    assertEquals ("client1", notification.attributes.get ("from"));
    assertEquals ("outgoing", notification.attributes.get ("Outgoing"));
    assertNull (notification.attributes.get ("Incoming"));
    
    // client 2 - > client 1
    federation.client2.sendNotify 
      (map ("federated", "server2", "from", "client2"));
    
    notification = (NotifyDeliver)federation.client1.receive ();
    
    assertEquals ("client2", notification.attributes.get ("from"));
    assertEquals ("incoming", notification.attributes.get ("Incoming"));
    assertNull (notification.attributes.get ("Outgoing"));
  }
  
  /**
   * Test secure delivery.
   */
  @Test
  public void security ()
    throws Exception
  {
    federation = new StandardFederatorSetup ();
    
    Key client1Private = new Key ("client1 private");
    Key client1Public = client1Private.publicKeyFor (SHA1_PRODUCER);
    
    Keys client1NtfnKeys = new Keys ();
    client1NtfnKeys.add (SHA1_PRODUCER, client1Private);
    
    Keys client2SubKeys = new Keys ();
    client2SubKeys.add (SHA1_PRODUCER, client1Public);
    
    federation.client1.sendAndReceive
      (new SecRqst (client1NtfnKeys, EMPTY_KEYS, EMPTY_KEYS, EMPTY_KEYS));
    
    federation.client2.sendAndReceive
      (new SecRqst (EMPTY_KEYS, EMPTY_KEYS, client2SubKeys, EMPTY_KEYS));
    
    // client 1 -> client 2
    federation.client1.send 
      (new NotifyEmit 
        (map ("federated", "server1", "from", "client1"), 
         false, EMPTY_KEYS));
    
    NotifyDeliver notification = (NotifyDeliver)federation.client2.receive ();
    
    assertEquals (1, notification.secureMatches.length);
    assertEquals (0, notification.insecureMatches.length);
    assertEquals ("client1", notification.attributes.get ("from"));
  }

  /**
   * Test that connector will keep trying to connect on initial failure.
   */
  @Test
  public void connectTimeout ()
    throws Exception
  {
    // Log.enableLogging (Log.DIAGNOSTIC, true);
    
    Router server1 = new Router (PORT1);
    Router server2 = new Router (PORT2);

    autoClose (server1);
    autoClose (server2);
    
    FederationClass fedClass =
      new FederationClass ("require (federated)", "require (federated)");
    
    FederationClasses classes = new FederationClasses (fedClass);
    
    EwafURI ewafURI = new EwafURI ("ewaf://localhost:" + (PORT1 + 1));
        
    // set connect/request timeout to 1 second
    Options options = new Options (FederationOptionSet.OPTION_SET);
    options.set ("Federation.Request-Timeout", 1);
    
    Connector connector = 
      new Connector (server1, "server1", ewafURI, 
                     fedClass, options);
    
    autoClose (connector);
    
    waitForAsyncConnect (connector);
    
    Acceptor acceptor = 
      new Acceptor (server2, "server2", classes, set (ewafURI), options);
    
    autoClose (acceptor);
    
    waitForConnect (connector);
  }
  
  /**
   * Test that connector will reconnect on disconnect.
   */
  @Test
  public void reconnect ()
    throws Exception
  {    
    Router server1 = new Router (PORT1);
    Router server2 = new Router (PORT2);

    autoClose (server1);
    autoClose (server2);
    
    FederationClass fedClass =
      new FederationClass ("require (federated)", "require (federated)");
    
    FederationClasses classes = new FederationClasses (fedClass);
    
    EwafURI ewafURI = new EwafURI ("ewaf://localhost:" + (PORT1 + 1));
        
    // set connect timeout to 1 second
    Options options = new Options (FederationOptionSet.OPTION_SET);
    options.set ("Federation.Request-Timeout", 1);
   
    // Log.enableLogging (Log.DIAGNOSTIC, true);
    // Log.enableLogging (Log.TRACE, true);

    Acceptor acceptor = 
      new Acceptor (server2, "server2", classes, set (ewafURI), options);
    
    autoClose (acceptor);
    
    Connector connector = 
      new Connector (server1, "server1", ewafURI, 
                     fedClass, options);

    autoClose (connector);
    
    waitForConnect (connector);
    
    acceptor.close ();
    
    waitForAsyncConnect (connector);

    acceptor = 
      new Acceptor (server2, "server2", classes, set (ewafURI), options);
    
    autoClose (acceptor);
    
    waitForConnect (connector);
  }
  
  @Test
  public void liveness ()
    throws Exception
  {    
    Router server1 = new Router (PORT1);
    Router server2 = new Router (PORT2);

    autoClose (server1);
    autoClose (server2);
    
    FederationClass fedClass =
      new FederationClass ("require (federated)", "require (federated)");
    
    FederationClasses classes = new FederationClasses (fedClass);
    
    EwafURI ewafURI = new EwafURI ("ewaf://localhost:" + (PORT1 + 1));
        
    // set connect timeout to 1 second
    Options options = new Options (FederationOptionSet.OPTION_SET);
    options.set ("Federation.Request-Timeout", 1);
    options.set ("Federation.Keepalive-Interval", 1);
   
    // Log.enableLogging (Log.DIAGNOSTIC, true);
    // Log.enableLogging (Log.TRACE, true);

    Acceptor acceptor = 
      new Acceptor (server2, "server2", classes, set (ewafURI), options);
    
    autoClose (acceptor);
    
    Connector connector = 
      new Connector (server1, "server1", ewafURI, fedClass, options);

    autoClose (connector);
    
    waitForConnect (connector);
    
    // "crash" link at acceptor end
    acceptor.hang ();
    
    // liveness check will generate a warning: ignore
    logTester.pause ();
    
    waitForAsyncConnect (connector);
    
    logTester.unpause ();
    
    acceptor.unhang ();
  }

  private static boolean runElvind = true;
  
  /**
   * Test against Mantara elvind.
   */
  @Test
  @Ignore
  public void mantara () 
    throws Exception
  {
    // Log.enableLogging (Log.TRACE, true);
    // Log.enableLogging (Log.DIAGNOSTIC, true);
    
    EwafURI ewafURI = new EwafURI ("ewaf:1.0//localhost:" + (PORT1 + 1));

    // run elvind
    Process elvind = null;
    
    if (runElvind)
      elvind = runMantaraElvind (ewafURI, 
                                 "require (federated)", "require (federated)");
    
    // start Avis on PORT2
    Router server = new Router (PORT2);
    autoClose (server);
    
    Options options = new Options (FederationOptionSet.OPTION_SET);
    
    FederationClass fedClass =
      new FederationClass ("require (federated)", "require (federated)");
    
    Connector connector = 
      new Connector (server, "avis", ewafURI, fedClass, options);
    
    autoClose (connector);

    waitForConnect (connector);

    // connect two clients, one to elvind, one to Avis
    SimpleClient client1 = new SimpleClient ("client1", "localhost", PORT1);
    SimpleClient client2 = new SimpleClient ("client2", "localhost", PORT2);
    
    autoClose (client1);
    autoClose (client2);
    
    client1.connect ();
    client2.connect ();
    
    // secure messaging: set up dual key scheme for two-way comms
    Key client1Private = new Key ("client1 private");
    Key client2Private = new Key ("client2 private");
    Key client1Public = client1Private.publicKeyFor (SHA1_PRODUCER);
    Key client2Public = client2Private.publicKeyFor (SHA1_PRODUCER);
    
    Keys client1NtfnKeys = new Keys ();
    client1NtfnKeys.add (SHA1_DUAL, PRODUCER, client1Private);
    client1NtfnKeys.add (SHA1_DUAL, CONSUMER, client2Public);
    
    Keys client1SubKeys = new Keys ();
    client1SubKeys.add (SHA1_DUAL, PRODUCER, client2Public);
    client1SubKeys.add (SHA1_DUAL, CONSUMER, client1Private);

    Keys client2NtfnKeys = new Keys ();
    client2NtfnKeys.add (SHA1_DUAL, PRODUCER, client2Private);
    client2NtfnKeys.add (SHA1_DUAL, CONSUMER, client1Public);

    Keys client2SubKeys = new Keys ();
    client2SubKeys.add (SHA1_DUAL, PRODUCER, client1Public);
    client2SubKeys.add (SHA1_DUAL, CONSUMER, client2Private);
    
    client1.sendAndReceive
      (new SecRqst (client1NtfnKeys, EMPTY_KEYS, client1SubKeys, EMPTY_KEYS));
    
    client2.sendAndReceive
      (new SecRqst (client2NtfnKeys, EMPTY_KEYS, client2SubKeys, EMPTY_KEYS));
    
    testTwoWayClientSendReceive (client1, client2, true);
    
    client1.subscribe ("require (federated) && from == 'client2'");
    client2.subscribe ("require (federated) && from == 'client1'");

    testTwoWayClientSendReceive (client1, client2);
    
    if (elvind != null)
      elvind.destroy ();
  }
  
  /**
   * Ad hoc test of AST IO against Mantara elvind.
   */
  @Test
  @Ignore
  public void mantaraAST () 
    throws Exception
  {
    // Log.enableLogging (Log.TRACE, true);
    // Log.enableLogging (Log.DIAGNOSTIC, true);
    
    EwafURI ewafURI = new EwafURI ("ewaf:1.0//localhost:" + (PORT1 + 1));

    Process elvind = null;
    
    String require =
      "require (federated) && size (from) > 2 && string (from) && " +
      "(equals (from, 'client1', 'client2') || " +
      " begins-with (from, 'c') || (fred + 1 > 42))";
    
    if (runElvind)
      elvind = runMantaraElvind (ewafURI, require, "TRUE");
    
    Router server = new Router (PORT2);
    autoClose (server);
    
    Options options = new Options (FederationOptionSet.OPTION_SET);
    
    FederationClass fedClass =
      new FederationClass (require, 
                           "require (federated)");
    
    Connector connector = 
      new Connector (server, "avis", ewafURI, 
                               fedClass, options);
    
    autoClose (connector);
    
    waitForConnect (connector);
    
    SimpleClient client1 = new SimpleClient ("client1", "localhost", PORT1);
    SimpleClient client2 = new SimpleClient ("client2", "localhost", PORT2);
    
    autoClose (client1);
    autoClose (client2);
    
    client1.connect ();
    client2.connect ();
    
    client1.subscribe ("require (federated) && from == 'client2'");
    client2.subscribe ("require (federated) && from == 'client1'");

    testTwoWayClientSendReceive (client1, client2);
    
    if (elvind != null)
      elvind.destroy ();
  }
  
  private static Process runMantaraElvind (EwafURI ewafURI, 
                                           String require, 
                                           String provide)
    throws Exception
  {
    File mantaraConfig = File.createTempFile ("elvin", "conf");
    mantaraConfig.deleteOnExit ();
    
    String config =
      "protocol elvin:4.0/tcp,none,xdr/0.0.0.0:" + PORT1 + "\n" +
      "federation yes\n" +
      "federation.name mantara\n" +
      "federation.protocol " + ewafURI + "\n" +
      "federation.class test\n" +
      "federation.subscribe test " + require + "\n" +
      "federation.provide test " + provide + "\n";
    
    Writer configStream = 
      new OutputStreamWriter (new FileOutputStream (mantaraConfig));
    configStream.append (config);
    configStream.close ();
    
    Process elvind = 
      new ProcessBuilder ().command 
        (MANTARA_ELVIN, "-l", 
         "-f", mantaraConfig.getAbsolutePath ()).start ();
    
    sleep (2000); // give elvind time to start
    
    return elvind;
  }
  
  private static void testTwoWayClientSendReceive (SimpleClient client1,
                                                   SimpleClient client2)
    throws Exception
  {
    testTwoWayClientSendReceive (client1, client2, false);
  }
  
  /**
   * Test client1 can see message from client2 and vice-versa.
   */
  private static void testTwoWayClientSendReceive (SimpleClient client1,
                                                   SimpleClient client2,
                                                   boolean secure)
    throws Exception
  {
    // client 1 -> client 2
    client1.sendNotify 
      (map ("federated", "server1", "from", "client1"));
    
    NotifyDeliver notification = (NotifyDeliver)client2.receive ();

    if (secure)
    {
      assertEquals (1, notification.secureMatches.length);
      assertEquals (0, notification.insecureMatches.length);
    } else
    {
      assertEquals (0, notification.secureMatches.length);
      assertEquals (1, notification.insecureMatches.length);
    }

    assertEquals ("client1", notification.attributes.get ("from"));
    
    // client 2 - > client 1
    client2.sendNotify 
      (map ("federated", "server2", "from", "client2"));
    
    notification = (NotifyDeliver)client1.receive ();
    
    if (secure)
    {
      assertEquals (1, notification.secureMatches.length);
      assertEquals (0, notification.insecureMatches.length);
    } else
    {
      assertEquals (0, notification.secureMatches.length);
      assertEquals (1, notification.insecureMatches.length);
    }
    
    assertEquals ("client2", notification.attributes.get ("from"));
  }

  private static Map<String, Object> map (String... nameValues)
  {
    HashMap<String, Object> map = new HashMap<String, Object> ();
    
    for (int i = 0; i < nameValues.length; i += 2)
      map.put (nameValues [i], nameValues [i + 1]);
    
    return map;
  }
  
  private void autoClose (Closeable toClose)
  {
    autoClose.add (toClose);
  }
  
  private void autoClose (final IoConnector connector)
  {
    autoClose (new Closeable ()
    {
      public void close ()
        throws IOException
      {
        connector.dispose ();
      }
    });
  }
  
  /**
   * Create a connection to federation listener.
   */
  private IoSession connectFederation (EwafURI uri,
                                       IoHandler listener)
    throws InterruptedException
  {
    SocketConnector connector = new NioSocketConnector (1);
    
    connector.setConnectTimeoutMillis (5000);
    
    connector.getFilterChain ().addLast ("codec", FederationFrameCodec.FILTER);
    
    connector.setHandler (listener);
    
    autoClose (connector);
    
    IoSession session = connector.connect 
      (new InetSocketAddress (uri.host, uri.port)).await ().getSession ();
    
    return session;
  }
}
