package org.avis.federation;

import java.util.HashMap;
import java.util.Map;

import org.avis.io.messages.NotifyDeliver;
import org.avis.router.Router;
import org.avis.router.SimpleClient;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.avis.federation.Federation.DEFAULT_EWAF_PORT;
import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;
import static org.avis.io.Net.addressesFor;
import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.enableLogging;
import static org.avis.util.Collections.set;

import static org.junit.Assert.assertEquals;

public class JUTestFederation
{
  private static final int PORT1 = 29170;
  private static final int PORT2 = 29180;
  
  private LogFailTester logTester;

  @Before
  public void setup ()
  {
    logTester = new LogFailTester ();
  }
  
  @After
  public void tearDown ()
  {
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
  @Ignore
  public void basic ()
    throws Exception
  {
    enableLogging (TRACE, true);
    enableLogging (DIAGNOSTIC, true);
    
    Router server1 = new Router (PORT1);
    Router server2 = new Router (PORT2);

//    FederationClass fedClass =
//      new FederationClass ("require (federation)", "true");
    FederationClass fedClass =
      new FederationClass ("require (federation)", "require (federation)");
    
    FederationClassMap federationMap = new FederationClassMap (fedClass);
    
    EwafURI ewafURI = new EwafURI ("ewaf://0.0.0.0:" + (PORT1 + 1));
    
    new FederationListener
      (server2, "server2", federationMap, addressesFor (set (ewafURI)));
    
//    Map<EwafURI, FederationClass> connectMap =
//      new HashMap<EwafURI, FederationClass> ();
    
//    connectMap.put (new EwafURI ("ewaf://0.0.0.0:" + (PORT1 + 1)), fedClass);
    
    new FederationConnector (server1, "server1", ewafURI, fedClass);
    
    SimpleClient client1 = new SimpleClient ("client1", "localhost", PORT1);
    SimpleClient client2 = new SimpleClient ("client2", "localhost", PORT2);
    
    client1.connect ();
    client2.connect ();
    
    client1.sendNotify (map ("federated", "client1"));
    
    NotifyDeliver notification = (NotifyDeliver)client2.receive ();
    
    assertEquals ("client1", notification.attributes.get ("federated"));
    
    client1.close ();
    client2.close ();
    
    // todo make sure federators auto close here also
    server1.close ();
    server2.close ();
  }

  private static Map<String, Object> map (String... nameValues)
  {
    HashMap<String, Object> map = new HashMap<String, Object> ();
    
    for (int i = 0; i < nameValues.length; i += 2)
      map.put (nameValues [i], nameValues [i + 1]);
    
    return map;
  }
}
