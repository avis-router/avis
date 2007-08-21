package org.avis.federation;

import java.util.HashMap;
import java.util.Map;

import java.net.InetAddress;

import org.avis.io.messages.NotifyDeliver;
import org.avis.router.Router;
import org.avis.router.SimpleClient;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Thread.sleep;

import static org.avis.io.Net.addressesFor;
import static org.avis.util.Collections.set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JUTestFederationManager
{
  private static final int PORT1 = 29170;
  private static final int PORT2 = PORT1 + 10;

  private LogFailTester logTester;

  @Before
  public void setup ()
  {
    // enableLogging (TRACE, true);
    // enableLogging (DIAGNOSTIC, true);

    logTester = new LogFailTester ();
  }
  
  @After
  public void tearDown ()
  {
    logTester.assertOkAndDispose ();
  }
  
  @Test
  public void basicListen () 
    throws Exception
  {
    EwafURI federationUri = new EwafURI ("ewaf://0.0.0.0:" + (PORT1 - 1));

    FederationOptions options = new FederationOptions ();
    
    options.set ("Federation.Router-Name", "router1");
    options.set ("Federation.Listen", federationUri);
    options.set ("Federation.Apply-Class[Test]", "router2 bogus");
    options.set ("Federation.Subscribe[Test]", "require (federated)");
    options.set ("Federation.Provide[Test]", "require (federated)");
    
    Router router1 = new Router (PORT1);
    
    FederationManager manager = new FederationManager (router1, options);
    
    FederationClass testClass = 
      manager.classMap.classFor (InetAddress.getLocalHost (), "router2");
    
    assertFalse (testClass.allowsNothing ());
    
    assertEquals (addressesFor (set (federationUri)),
                  manager.acceptor.addresses);
    
    Router router2 = new Router (PORT2);
    
    FederationClass fedClass =
      new FederationClass ("require (federated)", "require (federated)");
    
    Connector connector =
      new Connector (router2, "router2", federationUri, fedClass,
                               new FederationOptions ());
    
    sleep (1000);
    
    assertTrue (connector.isConnected ());
    
    assertEquals (1, manager.acceptor.links.size ());
    
    SimpleClient client1 = new SimpleClient ("client1", "localhost", PORT1);
    SimpleClient client2 = new SimpleClient ("client2", "localhost", PORT2);
    
    client1.connect ();
    client2.connect ();
    
    client1.subscribe ("require (federated) && from == 'client2'");
    client2.subscribe ("require (federated) && from == 'client1'");
    
    client1.sendNotify 
      (map ("federated", "router1", "from", "client1"));
    
    NotifyDeliver notification = (NotifyDeliver)client2.receive ();
    
    assertEquals (0, notification.secureMatches.length);
    assertEquals (1, notification.insecureMatches.length);
    assertEquals ("client1", notification.attributes.get ("from"));
    
    client1.close ();
    client2.close ();
    
    connector.close ();
    router2.close ();
    
    router1.close ();
    
    assertTrue (manager.isClosed ());
  }
  
  private static Map<String, Object> map (String... nameValues)
  {
    HashMap<String, Object> map = new HashMap<String, Object> ();
    
    for (int i = 0; i < nameValues.length; i += 2)
      map.put (nameValues [i], nameValues [i + 1]);
    
    return map;
  }
}
