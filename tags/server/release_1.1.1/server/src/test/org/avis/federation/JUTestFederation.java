package org.avis.federation;

import java.util.HashMap;
import java.util.Map;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.avis.config.Options;
import org.avis.io.messages.NotifyDeliver;
import org.avis.io.messages.NotifyEmit;
import org.avis.io.messages.SecRqst;
import org.avis.router.Router;
import org.avis.router.SimpleClient;
import org.avis.security.Key;
import org.avis.security.Keys;
import org.avis.subscription.parser.ParseException;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static java.lang.Thread.sleep;

import static org.avis.federation.Federation.DEFAULT_EWAF_PORT;
import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;
import static org.avis.io.Net.addressesFor;
import static org.avis.security.KeyScheme.SHA1_PRODUCER;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.util.Collections.set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JUTestFederation
{
  private static final int PORT1 = 29170;
  private static final int PORT2 = 29180;
  
  private static final String MANTARA_ELVIN = "/usr/local/sbin/elvind";
  
  private LogFailTester logTester;
  private StandardFederatorSetup federation;

  @Before
  public void setup ()
  {
    // enableLogging (TRACE, true);
    // enableLogging (DIAGNOSTIC, true);

    logTester = new LogFailTester ();
  }
  
  @After
  public void tearDown ()
    throws Exception
  {
    logTester.assertOkAndDispose ();
    
    if (federation != null)
    {
      federation.close ();
      federation = null;
    }
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
  public void basic ()
    throws Exception
  {
    federation = new StandardFederatorSetup ();
    
    testTwoWayClientSendReceive (federation.client1, federation.client2);
    
    federation.close ();
  }
  
  @Test
  public void addAttributes ()
    throws Exception
  {
    FederationClass fedClass1 = StandardFederatorSetup.defaultClass ();
    FederationClass fedClass2 = StandardFederatorSetup.defaultClass ();
    
    FederationClasses classes = new FederationClasses ();
    
    classes.mapServerDomain ("server2", fedClass1);
    classes.mapServerDomain ("server1", fedClass2);
    
    fedClass1.incomingAttributes = new HashMap<String, Object> ();
    fedClass1.outgoingAttributes = new HashMap<String, Object> ();
    
    fedClass1.incomingAttributes.put ("Incoming", "incoming");
    fedClass1.outgoingAttributes.put ("Outgoing", "outgoing");
    
    federation = new StandardFederatorSetup (classes);
    
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
    
    federation.close ();
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
    
    federation.close ();
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
    
    sleep (2000);
    
    // check we're waiting
    assertTrue (connector.isWaitingForAsyncConnection ());
    
    Acceptor acceptor = 
      new Acceptor (server2, "server2", classes, 
                    addressesFor (set (ewafURI)), options);
    
    sleep (2000);
    
    // check we've connected
    assertTrue (connector.isConnected ());
    
    connector.close ();
    acceptor.close ();
    
    server1.close ();
    server2.close ();
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
      new Acceptor (server2, "server2", classes, 
                    addressesFor (set (ewafURI)), options);

    Connector connector = 
      new Connector (server1, "server1", ewafURI, 
                     fedClass, options);

    sleep (1000);
    
    // check we've connected
    assertTrue (connector.isConnected ());

    acceptor.close ();
    
    sleep (3000);

    // check we're waiting
    assertTrue (connector.isWaitingForAsyncConnection ());
    
    acceptor = 
      new Acceptor (server2, "server2", classes, 
                    addressesFor (set (ewafURI)), options);
    
    sleep (2000);
    
    // check we've reconnected
    assertTrue (connector.isConnected ());
    
    connector.close ();
    acceptor.close ();
    
    server1.close ();
    server2.close ();
  }
  
  @Test
  public void liveness ()
    throws Exception
  {    
    Router server1 = new Router (PORT1);
    Router server2 = new Router (PORT2);

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
      new Acceptor (server2, "server2", classes, 
                    addressesFor (set (ewafURI)), options);

    Connector connector = 
      new Connector (server1, "server1", ewafURI, 
                     fedClass, options);

    sleep (1000);
    
    // check we've connected
    assertTrue (connector.isConnected ());

    // "crash" link at acceptor end
    acceptor.hang ();
    
    // liveness check will generate a warning: ignore
    logTester.pause ();
    
    // wait for other end to notice
    sleep (8000);
    
    logTester.unpause ();
    
    assertTrue (connector.isWaitingForAsyncConnection ());
    
    connector.close ();
    acceptor.close ();
    
    server1.close ();
    server2.close ();
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

    Process elvind = null;
    
    if (runElvind)
      elvind = runMantaraElvind (ewafURI, 
                                 "require (federated)", "require (federated)");
    
    Router server = new Router (PORT2);

    Options options = new Options (FederationOptionSet.OPTION_SET);
    
    FederationClass fedClass =
      new FederationClass ("require (federated)", "require (federated)");
    
    Connector connector = 
      new Connector (server, "avis", ewafURI, 
                               fedClass, options);
    
    SimpleClient client1 = new SimpleClient ("client1", "localhost", PORT1);
    SimpleClient client2 = new SimpleClient ("client2", "localhost", PORT2);
    
    client1.connect ();
    client2.connect ();
    
    client1.subscribe ("require (federated) && from == 'client2'");
    client2.subscribe ("require (federated) && from == 'client1'");

    testTwoWayClientSendReceive (client1, client2);
    
    client1.close ();
    client2.close ();
    
    connector.close ();
    
    server.close ();
    
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

    Options options = new Options (FederationOptionSet.OPTION_SET);
    
    FederationClass fedClass =
      new FederationClass (require, 
                           "require (federated)");
    
    Connector connector = 
      new Connector (server, "avis", ewafURI, 
                               fedClass, options);
    
    SimpleClient client1 = new SimpleClient ("client1", "localhost", PORT1);
    SimpleClient client2 = new SimpleClient ("client2", "localhost", PORT2);
    
    client1.connect ();
    client2.connect ();
    
    client1.subscribe ("require (federated) && from == 'client2'");
    client2.subscribe ("require (federated) && from == 'client1'");

    testTwoWayClientSendReceive (client1, client2);
    
    client1.close ();
    client2.close ();
    
    connector.close ();
    
    server.close ();
    
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
  
  /**
   * Test client1 can see message from client2 and vice-versa.
   */
  private static void testTwoWayClientSendReceive (SimpleClient client1,
                                                   SimpleClient client2)
    throws Exception
  {
    // client 1 -> client 2
    client1.sendNotify 
      (map ("federated", "server1", "from", "client1"));
    
    NotifyDeliver notification = (NotifyDeliver)client2.receive ();
    
    assertEquals ("client1", notification.attributes.get ("from"));
    
    // client 2 - > client 1
    client2.sendNotify 
      (map ("federated", "server2", "from", "client2"));
    
    notification = (NotifyDeliver)client1.receive ();
    
    assertEquals (0, notification.secureMatches.length);
    assertEquals (1, notification.insecureMatches.length);
    assertEquals ("client2", notification.attributes.get ("from"));
  }

  private static Map<String, Object> map (String... nameValues)
  {
    HashMap<String, Object> map = new HashMap<String, Object> ();
    
    for (int i = 0; i < nameValues.length; i += 2)
      map.put (nameValues [i], nameValues [i + 1]);
    
    return map;
  }
  
  static class StandardFederatorSetup
  {
    public Router server1;
    public Router server2;
    
    public SimpleClient client1;
    public SimpleClient client2;
    
    public Connector connector;
    public Acceptor acceptor;
    
    public StandardFederatorSetup ()
      throws Exception
    {
      this (new Options (FederationOptionSet.OPTION_SET));
    }
    
    public StandardFederatorSetup (FederationClasses classes)
      throws Exception
    {
      this (classes, new Options (FederationOptionSet.OPTION_SET));
    }
    
    public StandardFederatorSetup (Options options)
      throws Exception
    {
      this (new FederationClasses (defaultClass ()), options);
    }

    public static FederationClass defaultClass ()
      throws ParseException
    {
      return new FederationClass ("require (federated)",
                                  "require (federated)");
    }
    
    public StandardFederatorSetup (FederationClasses classes,
                                   Options options)
      throws Exception
    {
      server1 = new Router (PORT1);
      server2 = new Router (PORT2);

      EwafURI ewafURI = new EwafURI ("ewaf://localhost:" + (PORT1 + 1));
      
      acceptor = 
        new Acceptor (server2, "server2", classes, 
                      addressesFor (set (ewafURI)), options);
      
      connector = 
        new Connector (server1, "server1", ewafURI, 
                       classes.classFor ("server2"), options);
      
      client1 = new SimpleClient ("client1", "localhost", PORT1);
      client2 = new SimpleClient ("client2", "localhost", PORT2);
      
      client1.connect ();
      client2.connect ();
      
      client1.subscribe ("require (federated) && from == 'client2'");
      client2.subscribe ("require (federated) && from == 'client1'");
    }
    
    public void close ()
      throws Exception
    {
      client1.close ();
      client2.close ();
      
      connector.close ();
      acceptor.close ();
      
      server1.close ();
      server2.close ();
    }
  }
}