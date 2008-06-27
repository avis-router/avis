package org.avis.net.server;

import java.util.HashMap;
import java.util.Map;

import org.avis.Notification;
import org.avis.net.messages.ConfConn;
import org.avis.net.messages.ConnRply;
import org.avis.net.messages.ConnRqst;
import org.avis.net.messages.DisconnRply;
import org.avis.net.messages.DisconnRqst;
import org.avis.net.messages.Nack;
import org.avis.net.messages.NotifyDeliver;
import org.avis.net.messages.NotifyEmit;
import org.avis.net.messages.SecRqst;
import org.avis.net.messages.SubAddRqst;
import org.avis.net.messages.SubDelRqst;
import org.avis.net.messages.SubModRqst;
import org.avis.net.messages.SubRply;
import org.avis.net.messages.TestConn;
import org.avis.net.messages.UNotify;
import org.avis.net.security.Key;
import org.avis.net.security.Keys;

import org.junit.After;
import org.junit.Test;

import static org.avis.net.security.KeyScheme.SHA1_PRODUCER;
import static org.avis.net.security.Keys.EMPTY_KEYS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the router server.
 * 
 * @author Matthew Phillips
 */
public class JUTestServer
{
  static final int PORT = 29170;

  private Server server;

  @After public void tearDown ()
  {
    if (server != null)
      server.close ();
  }
  
  @Test public void connect ()
    throws Exception
  {
    server = new Server (PORT);
    SimpleClient client = new SimpleClient ();
    
    ConnRqst connRqst = new ConnRqst (4, 0);
    client.send (connRqst);
    
    ConnRply reply = (ConnRply)client.receive ();
    
    assertEquals (connRqst.xid, reply.xid);
    
    DisconnRqst disconnRqst = new DisconnRqst ();
    client.send (disconnRqst);
    
    DisconnRply disconnRply = (DisconnRply)client.receive ();
    assertEquals (disconnRqst.xid, disconnRply.xid);
    
    client.close ();
    server.close ();
  }
  
  /**
   * Test connection options.
   * 
   * @see JUTestConnectionOptions
   */
  @Test
  public void connectionOptions ()
    throws Exception
  {
    server = new Server (PORT);
    SimpleClient client = new SimpleClient ("localhost", PORT);
    
    HashMap<String, Object> options = new HashMap<String, Object> ();
    options.put ("Attribute.Opaque.Max-Length", 2048 * 1024);
    options.put ("Network.Coalesce-Delay", 0);
    options.put ("Bogus", "not valid");
    
    ConnRqst connRqst = new ConnRqst (4, 0, options);
    client.send (connRqst);
    
    ConnRply reply = (ConnRply)client.receive ();
    
    assertEquals (connRqst.xid, reply.xid);
    
    System.out.println ("options = " + reply.options);
    assertEquals (2048 * 1024, reply.options.get ("Attribute.Opaque.Max-Length"));
    assertEquals (0, reply.options.get ("Network.Coalesce-Delay"));
    assertNull (reply.options.get ("Bogus"));
    
    client.close ();
    server.close ();
  }
  
  /**
   * Use the simple client to run through a connect, subscribe, emit,
   * change sub, disconnect sequence.
   */
  @Test public void subscribe ()
    throws Exception
  {
    server = new Server (PORT);
    SimpleClient client = new SimpleClient ();
    
    client.connect ();
    
    SubAddRqst subAddRqst = new SubAddRqst ("number == 1");
    client.send (subAddRqst);
    
    SubRply subReply = (SubRply)client.receive ();
    assertEquals (subAddRqst.xid, subReply.xid);
    
    // check NACK on bad subscription
    subAddRqst = new SubAddRqst ("(1 + 1");
    client.send (subAddRqst);
    
    Nack nackReply = (Nack)client.receive ();
    assertEquals (Nack.PARSE_ERROR, nackReply.error);
    
    // send notification
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("name", "foobar");
    ntfn.put ("number", 1);
    
    client.send (new NotifyEmit (ntfn));
    NotifyDeliver notifyDeliver = (NotifyDeliver)client.receive ();
    
    assertEquals (0, notifyDeliver.secureMatches.length);
    assertEquals (1, notifyDeliver.insecureMatches.length);
    assertEquals (subReply.subscriptionId, notifyDeliver.insecureMatches [0]);
    assertEquals ("foobar", notifyDeliver.attributes.get ("name"));
    assertEquals (1, notifyDeliver.attributes.get ("number"));
    
    // send non-matching ntfn
    ntfn = new HashMap<String, Object> ();
    ntfn.put ("name", "foobar");
    ntfn.put ("number", 2);
    
    // should get no reply to next: following tests will fail if not
    client.send (new NotifyEmit (ntfn));
    
    // modify subscription
    SubModRqst subModRqst = new SubModRqst (subReply.subscriptionId, "number == 2");
    client.send (subModRqst);
    
    subReply = (SubRply)client.receive ();
    assertEquals (subReply.xid, subModRqst.xid);
    assertEquals (subReply.subscriptionId, subModRqst.subscriptionId);
    
    // remove subscription
    SubDelRqst delRqst = new SubDelRqst (subReply.subscriptionId);
    client.send (delRqst);
    subReply = (SubRply)client.receive ();
    assertEquals (subReply.subscriptionId, delRqst.subscriptionId);
    
    // check NACK on remove invalid subscription
    delRqst = new SubDelRqst (subReply.subscriptionId);
    client.send (delRqst);
    nackReply = (Nack)client.receive ();
    assertEquals (Nack.NO_SUCH_SUB, nackReply.error);
    
    // send a connection test
    client.send (new TestConn ());
    assertTrue (client.receive () instanceof ConfConn);
    
    client.close ();
    server.close ();
  }
  
  /**
   * Test multiple clients sending messages between each other.
   */
  @Test public void multiClient ()
    throws Exception
  {
    server = new Server (PORT);
    
    // client 1
    SimpleClient client1 = new SimpleClient ();
    
    client1.connect ();
    
    SubAddRqst subAddRqst1 = new SubAddRqst ("client == 1 || all == 1");
    client1.send (subAddRqst1);
    
    SubRply subReply1 = (SubRply)client1.receive ();
    assertEquals (subAddRqst1.xid, subReply1.xid);
    
    // client 2
    SimpleClient client2 = new SimpleClient ();
    
    client2.connect ();
    
    SubAddRqst subAddRqst2 = new SubAddRqst ("client == 2 || all == 1");
    client2.send (subAddRqst2);
    
    SubRply subReply2 = (SubRply)client2.receive ();
    assertEquals (subAddRqst2.xid, subReply2.xid);
    
    // client 1 send message to client 2
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("client", 2);
    ntfn.put ("payload", "hello from client 1");
    
    client1.send (new NotifyEmit (ntfn));
    
    NotifyDeliver client2Notify = (NotifyDeliver)client2.receive ();
    assertEquals ("hello from client 1", client2Notify.attributes.get ("payload"));
    
    // client 2 send message to client 1
    ntfn = new HashMap<String, Object> ();
    ntfn.put ("client", 1);
    ntfn.put ("payload", "hello from client 2");
    
    client2.send (new NotifyEmit (ntfn));
    
    NotifyDeliver client1Notify = (NotifyDeliver)client1.receive ();
    assertEquals ("hello from client 2", client1Notify.attributes.get ("payload"));
    
    // client 1 sends message to all
    ntfn = new HashMap<String, Object> ();
    ntfn.put ("all", 1);
    ntfn.put ("payload", "hello all");
    
    client1.send (new NotifyEmit (ntfn));
    
    client2Notify = (NotifyDeliver)client2.receive ();
    assertEquals ("hello all", client2Notify.attributes.get ("payload"));
    
    client1Notify = (NotifyDeliver)client1.receive ();
    assertEquals ("hello all", client1Notify.attributes.get ("payload"));
    
    client1.close ();
    client2.close ();
    server.close ();
  }
  
  /**
   * Test secure messaging using the producer key scheme. Other
   * schemes should really be tested, but the key matching logic for
   * all the schemes supported in the server is done by the security
   * tests, so not bothering for now.
   */
  @Test public void security ()
    throws Exception
  {
    server = new Server (PORT);
    
//    SimpleClient alice = new SimpleClient ("localhost", 2917);
//    SimpleClient bob = new SimpleClient ("localhost", 2917);
//    SimpleClient eve = new SimpleClient ("localhost", 2917);
    
    SimpleClient alice = new SimpleClient ("alice");
    SimpleClient bob = new SimpleClient ("bob");
    SimpleClient eve = new SimpleClient ("eve");
    
    alice.connect ();
    bob.connect ();
    eve.connect ();

    Key alicePrivate = new Key ("alice private");
    Key alicePublic = alicePrivate.publicKeyFor (SHA1_PRODUCER);
    
    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_PRODUCER, alicePrivate);
    
    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_PRODUCER, alicePublic);
    
    Keys eveSubKeys = new Keys ();
    eveSubKeys.add (SHA1_PRODUCER,
                    new Key ("Not alice's key").publicKeyFor (SHA1_PRODUCER));
    
    bob.subscribe ("require (From-Alice)", bobSubKeys);
    
    eve.subscribe ("require (From-Alice)", eveSubKeys);
    
    Notification ntfn = new Notification ();
    ntfn.put ("From-Alice", 1);
    ntfn.keys = aliceNtfnKeys;
    
    alice.emitNotify (ntfn, false);
    
    NotifyDeliver bobNtfn = (NotifyDeliver)bob.receive ();
    assertEquals (1, bobNtfn.attributes.get ("From-Alice"));
    
    try
    {
      NotifyDeliver eveNtfn = (NotifyDeliver)eve.receive (2000);
      
      assertEquals (1, eveNtfn.attributes.get ("From-Alice"));
      
      fail ("Eve foiled our super secret scheme");
    } catch (MessageTimeoutException ex)
    {
      // ok
    }
    
    alice.close ();
    bob.close ();
    eve.close ();
  }
  
  @Test public void unotify ()
    throws Exception
  {
    server = new Server (PORT);
    SimpleClient client1 = new SimpleClient ();
    SimpleClient client2 = new SimpleClient ();
    
    client2.connect ();
    client2.subscribe ("number == 1");
    
    Notification ntfn = new Notification ();
    ntfn.put ("number", 1);
    ntfn.put ("client", "client 1");
    
    client1.send (new UNotify (4, 0, ntfn));
    client1.close ();
    
    NotifyDeliver reply = (NotifyDeliver)client2.receive ();
    assertEquals ("client 1", reply.attributes.get ("client"));
    
    client2.close ();
  }
  
  /**
   * Test handling of client that does Bad Things.
   */
  @Test public void badClient ()
    throws Exception
  {
    server = new Server (PORT);
    SimpleClient client = new SimpleClient ();
    SimpleClient badClient = new SimpleClient ();
    
    client.connect ();
    client.subscribe ("number == 1");
    
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("name", "foobar");
    ntfn.put ("number", 1);
    
    badClient.send (new NotifyEmit (ntfn));
    
    try
    {
      client.receive (2000);
      
      fail ("Server allowed client with no connection to notify");
    } catch (MessageTimeoutException ex)
    {
      // ok
    }
    
    // try change security with no connection
    SecRqst secRqst = new SecRqst (EMPTY_KEYS, EMPTY_KEYS, EMPTY_KEYS, EMPTY_KEYS);
    badClient.send (secRqst);
    Nack nack = (Nack)badClient.receive ();

    // try subscription with no connection
    SubAddRqst subAddRqst = new SubAddRqst ("require (hello)", EMPTY_KEYS);
    badClient.send (subAddRqst);
    nack = (Nack)badClient.receive ();
    assertEquals (subAddRqst.xid, nack.xid);
    
    // try to connect twice
    badClient.connect ();
    ConnRqst connRqst = new ConnRqst (4, 0);
    badClient.send (connRqst);
    nack = (Nack)badClient.receive ();
    assertEquals (connRqst.xid, nack.xid);
    
    // modify non-existent sub
    SubModRqst subModRqst = new SubModRqst (123456, "");
    badClient.send (subModRqst);
    nack = (Nack)badClient.receive ();
    assertEquals (subModRqst.xid, nack.xid);
    
    badClient.close ();
    client.close ();
  }
}