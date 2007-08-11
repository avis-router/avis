package org.avis.federation;

import java.util.HashMap;
import java.util.Map;

import org.avis.io.messages.NotifyDeliver;
import org.avis.io.messages.NotifyEmit;
import org.avis.io.messages.SecRqst;
import org.avis.logging.Log;
import org.avis.router.Router;
import org.avis.router.SimpleClient;
import org.avis.security.Key;
import org.avis.security.Keys;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
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
import static org.junit.Assert.assertTrue;

public class JUTestFederation
{
  private static final int PORT1 = 29170;
  private static final int PORT2 = 29180;
  
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
    StandardFederatorSetup federation = new StandardFederatorSetup ();
    
    // client 1 -> client 2
    federation.client1.sendNotify 
      (map ("federated", "server1", "from", "client1"));
    
    NotifyDeliver notification = (NotifyDeliver)federation.client2.receive ();
    
    assertEquals ("client1", notification.attributes.get ("from"));
    
    // client 2 - > client 1
    federation.client2.sendNotify 
      (map ("federated", "server2", "from", "client2"));
    
    notification = (NotifyDeliver)federation.client1.receive ();
    
    assertEquals (0, notification.secureMatches.length);
    assertEquals (1, notification.insecureMatches.length);
    assertEquals ("client2", notification.attributes.get ("from"));
    
    federation.close ();
  }
  
  /**
   * Test secure delivery.
   */
  @Test
  public void security ()
    throws Exception
  {
    StandardFederatorSetup federation = new StandardFederatorSetup ();
    
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
    
    FederationClassMap federationMap = new FederationClassMap (fedClass);
    
    EwafURI ewafURI = new EwafURI ("ewaf://localhost:" + (PORT1 + 1));
        
    // set connect timeout to 1 second
    FederationOptions options = new FederationOptions ();
    options.set ("Federation.Connection-Timeout", 1);
    
    FederationConnector connector = 
      new FederationConnector (server1, "server1", ewafURI, 
                               fedClass, options);
    
    sleep (2000);
    
    // check we're waiting
    assertTrue (connector.isWaitingForAsyncConnection ());
    
    FederationListener listener = 
      new FederationListener (server2, "server2", federationMap, 
                              addressesFor (set (ewafURI)));

    sleep (2000);
    
    // check we've connected
    assertTrue (connector.isConnected ());
    
    connector.close ();
    listener.close ();
    
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
    
    FederationClassMap federationMap = new FederationClassMap (fedClass);
    
    EwafURI ewafURI = new EwafURI ("ewaf://localhost:" + (PORT1 + 1));
        
    // set connect timeout to 1 second
    FederationOptions options = new FederationOptions ();
    options.set ("Federation.Connection-Timeout", 1);
   
    Log.enableLogging (Log.DIAGNOSTIC, true);
    Log.enableLogging (Log.TRACE, true);

    FederationListener listener = 
      new FederationListener (server2, "server2", federationMap, 
                              addressesFor (set (ewafURI)));

    FederationConnector connector = 
      new FederationConnector (server1, "server1", ewafURI, 
                               fedClass, options);

    sleep (1000);
    
    // check we've connected
    assertTrue (connector.isConnected ());

    listener.close ();
    
    sleep (3000);

    // check we're waiting
    assertTrue (connector.isWaitingForAsyncConnection ());
    
    listener = 
      new FederationListener (server2, "server2", federationMap, 
                              addressesFor (set (ewafURI)));
    
    sleep (2000);
    
    // check we've reconnected
    assertTrue (connector.isConnected ());
    
    connector.close ();
    listener.close ();
    
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
  
  static class StandardFederatorSetup
  {
    public Router server1;
    public Router server2;
    
    public SimpleClient client1;
    public SimpleClient client2;
    
    public FederationConnector connector;
    public FederationListener listener;
    
    public StandardFederatorSetup ()
      throws Exception
    {
      this (new FederationOptions ());
    }
    
    public StandardFederatorSetup (FederationOptions options)
      throws Exception
    {
      server1 = new Router (PORT1);
      server2 = new Router (PORT2);

      // FederationClass fedClass =
      //   new FederationClass ("require (federated)", "true");
      FederationClass fedClass =
        new FederationClass ("require (federated)", "require (federated)");
      
      FederationClassMap federationMap = new FederationClassMap (fedClass);
      
      EwafURI ewafURI = new EwafURI ("ewaf://localhost:" + (PORT1 + 1));
      
      listener = 
        new FederationListener (server2, "server2", federationMap, 
                                addressesFor (set (ewafURI)));
      
      connector = 
        new FederationConnector (server1, "server1", ewafURI, 
                                 fedClass, options);
      
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
      
      // todo make sure federators auto close
      connector.close ();
      listener.close ();
      
      server1.close ();
      server2.close ();
    }
  }
}
