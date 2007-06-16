package org.avis.client;

import java.io.IOException;

import org.avis.common.ElvinURI;
import org.avis.logging.Log;
import org.avis.security.Key;
import org.avis.security.Keys;
import org.avis.server.Server;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static java.lang.System.currentTimeMillis;

import static org.avis.client.CloseEvent.REASON_CLIENT_SHUTDOWN;
import static org.avis.client.CloseEvent.REASON_ROUTER_SHUTDOWN;
import static org.avis.client.InvalidSubscriptionException.SYNTAX_ERROR;
import static org.avis.client.InvalidSubscriptionException.TRIVIAL_EXPRESSION;
import static org.avis.client.SecureMode.ALLOW_INSECURE_DELIVERY;
import static org.avis.client.SecureMode.REQUIRE_SECURE_DELIVERY;
import static org.avis.security.KeyScheme.SHA1_PRODUCER;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.util.Collections.set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JUTestClient
{
  private static final String ELVIN_URI = "elvin://localhost:29170";
  
  private Server server;
  private LogFailTester logTester;

  @Before
  public void setup ()
  {
    logTester = new LogFailTester ();
    Log.enableLogging (Log.TRACE, false);
  }
  
  @After
  public void cleanup ()
  {
    if (server != null)
      server.close ();
    
    logTester.assertOkAndDispose ();
  }
  
  private void createServer ()
    throws IOException
  {
    server = new Server (29170);
  }
  
  /**
   * Test that client handles changing subs in a notification callback.
   */
  @Test
  public void modifySubInNotifyEvent ()
    throws Exception
  {
    createServer ();
    
    final Elvin client = new Elvin (ELVIN_URI);
    
    final Subscription sub = client.subscribe ("require (test)");
    
    assertTrue (client.hasSubscription (sub));
    
    sub.addListener (new NotificationListener ()
    {
      public void notificationReceived (NotificationEvent e)
      {
        assertFalse (e.secure);
        
        try
        {
          sub.remove ();
          
          synchronized (sub)
          {
            sub.notifyAll ();
          }
        } catch (IOException ex)
        {
          throw new RuntimeException (ex);
        }
      }
    });
    
    Notification ntfn = new Notification ();
    ntfn.set ("test", 1);
    ntfn.set ("payload", "test 1");
    
    synchronized (sub)
    {
      client.send (ntfn);
      
      wait (sub);
    }
    
    assertFalse (client.hasSubscription (sub));
    
    client.close ();
    server.close ();
  }
  
  /**
   * Test that client handles sending a notificaion in a notification
   * callback.
   */
  @Test
  public void notifyInNotifyEvent ()
    throws Exception
  {
    createServer ();
    
    final Elvin client = new Elvin (ELVIN_URI);
    final Subscription sub = client.subscribe ("require (test)");
    
    sub.addListener (new NotificationListener ()
    {
      public void notificationReceived (NotificationEvent e)
      {
        assertFalse (e.secure);
        
        try
        {
          if (e.notification.get ("payload").equals ("test 1"))
          {
            Notification ntfn = new Notification ();
            ntfn.set ("test", 1);
            ntfn.set ("payload", "test 2");
            
            client.send (ntfn);
          } else
          {
            synchronized (sub)
            {
              sub.notifyAll ();
            }
          }
        } catch (IOException ex)
        {
          throw new RuntimeException (ex);
        }
      }
    });
    
    Notification ntfn = new Notification ();
    ntfn.set ("test", 1);
    ntfn.set ("payload", "test 1");
    
    synchronized (sub)
    {
      client.send (ntfn);
      
      wait (sub);
    }
    
    client.close ();
    server.close ();
  }

  @Test
  public void security ()
    throws Exception
  {
    createServer ();
    
    ElvinURI uri = new ElvinURI (ELVIN_URI);
    ConnectionOptions options = new ConnectionOptions ();
    
    Key alicePrivate = new Key ("alice private");

    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_PRODUCER, alicePrivate);
    
    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_PRODUCER, alicePrivate.publicKeyFor (SHA1_PRODUCER));
    
    // subscribe with global keys
    Elvin aliceClient = new Elvin (uri, options, aliceNtfnKeys, EMPTY_KEYS);
    Elvin bobClient = new Elvin (uri, options, EMPTY_KEYS, bobSubKeys);
    Elvin eveClient = new Elvin (uri, options);
    
    Subscription bobSub =
      bobClient.subscribe ("require (From-Alice)", REQUIRE_SECURE_DELIVERY);
    
    Subscription eveSub = eveClient.subscribe ("require (From-Alice)");
    
    checkSecureSendReceive (aliceClient, bobSub, eveSub);
    
    aliceClient.close ();
    bobClient.close ();
    eveClient.close ();
    
    // check we can add global keys later for same result
    aliceClient = new Elvin (uri);
    bobClient = new Elvin (uri);
    eveClient = new Elvin (uri);
    
    aliceClient.setNotificationKeys (aliceNtfnKeys);
    
    bobSub = bobClient.subscribe ("require (From-Alice)",
                                  REQUIRE_SECURE_DELIVERY);
    bobClient.setSubscriptionKeys (bobSubKeys);
    
    eveSub = eveClient.subscribe ("require (From-Alice)");
    
    checkSecureSendReceive (aliceClient, bobSub, eveSub);
    
    aliceClient.close ();
    bobClient.close ();
    eveClient.close ();
    
    // check we can add subscription keys for same result
    aliceClient = new Elvin (uri, options, aliceNtfnKeys, EMPTY_KEYS);
    bobClient = new Elvin (uri);
    eveClient = new Elvin (uri);
    
    bobSub = bobClient.subscribe ("require (From-Alice)",
                                  REQUIRE_SECURE_DELIVERY);
    
    bobSub.setKeys (bobSubKeys);
    
    eveSub = eveClient.subscribe ("require (From-Alice)");
    
    checkSecureSendReceive (aliceClient, bobSub, eveSub);
    
    aliceClient.close ();
    bobClient.close ();
    eveClient.close ();
    
    // check we can subscribe securely in one step
    aliceClient = new Elvin (uri, options, aliceNtfnKeys, EMPTY_KEYS);
    bobClient = new Elvin (uri);
    eveClient = new Elvin (uri);
    
    bobSub = bobClient.subscribe ("require (From-Alice)",
                                  bobSubKeys, REQUIRE_SECURE_DELIVERY);
    
    eveSub = eveClient.subscribe ("require (From-Alice)");
    
    checkSecureSendReceive (aliceClient, bobSub, eveSub);
    
    aliceClient.close ();
    bobClient.close ();
    eveClient.close ();
  }
  
  /**
   * Test changing secure mode after subscription.
   */
  @Test
  public void secureMode ()
    throws Exception
  {
    createServer ();
    
    ElvinURI uri = new ElvinURI (ELVIN_URI);
    
    Key alicePrivate = new Key ("alice private");

    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_PRODUCER, alicePrivate);
    
    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_PRODUCER, alicePrivate.publicKeyFor (SHA1_PRODUCER));
    
    Elvin aliceClient = new Elvin (uri, EMPTY_KEYS, EMPTY_KEYS);
    Elvin bobClient = new Elvin (uri, EMPTY_KEYS, bobSubKeys);
    
    Subscription bobSub = bobClient.subscribe ("require (From-Alice)");
    
    TestNtfnListener listener = new TestNtfnListener (bobSub);
    
    // change secure mode after subscription
    bobSub.setSecureMode (REQUIRE_SECURE_DELIVERY);
    
    Notification notification = new Notification ();
    notification.set ("From-Alice", 1);
    
    // send insecure, bob should not get it
    aliceClient.send (notification);
    
    listener.waitForNoNotification ();
    
    listener.reset ();
    
    // send secure
    aliceClient.send (notification, aliceNtfnKeys);
    
    listener.waitForNotification ();
    
    assertTrue (listener.event.secure);
    
    aliceClient.close ();
    bobClient.close ();
  }
  
  @Test
  public void subscribe ()
    throws Exception
  {
    createServer ();
    
    final Elvin client = new Elvin (ELVIN_URI);
    final Subscription sub = client.subscribe ("require (test)");
    
    // change subscription
    sub.setSubscriptionExpr ("require (test2)");
    
    sub.addListener (new NotificationListener ()
    {
      public void notificationReceived (NotificationEvent e)
      {
        synchronized (sub)
        {
          sub.notifyAll ();
        }
      }
    });
    
    Notification ntfn = new Notification ();
    ntfn.set ("test2", 1);
    
    synchronized (sub)
    {
      client.send (ntfn);
      
      wait (sub);
    }
  }

  /**
   * Test subscription-specific and general (connection-wide)
   * subscription listeners.
   */
  @Test
  public void subscriptionListeners ()
    throws Exception
  {
    createServer ();
    
    Key privateKey = new Key ("private");

    Keys secureNtfnKeys = new Keys ();
    secureNtfnKeys.add (SHA1_PRODUCER, privateKey);
    
    Keys secureSubKeys = new Keys ();
    secureSubKeys.add (SHA1_PRODUCER, privateKey.publicKeyFor (SHA1_PRODUCER));
    
    Elvin elvin = new Elvin (ELVIN_URI);
    Subscription secureSub =
      elvin.subscribe ("require (test)", secureSubKeys, REQUIRE_SECURE_DELIVERY);
    Subscription semiSecureSub =
      elvin.subscribe ("require (test)", secureSubKeys, ALLOW_INSECURE_DELIVERY);
    Subscription insecureSub = elvin.subscribe ("require (test)");
    Subscription multiSub =
      elvin.subscribe ("require (test) || require (frob)");
    
    TestNtfnListener secureSubListener = new TestNtfnListener (secureSub);
    TestNtfnListener semiSecureSubListener = new TestNtfnListener (semiSecureSub);
    TestNtfnListener insecureSubListener = new TestNtfnListener (insecureSub);
    TestNtfnListener multiSubListener = new TestNtfnListener (multiSub);
    TestGeneralNtfnListener generalListener = new TestGeneralNtfnListener (elvin);
    
    Notification ntfn = new Notification ("test", 1);
    
    // send insecure
    elvin.send (ntfn);
    
    insecureSubListener.waitForNotification ();
    semiSecureSubListener.waitForNotification ();
    multiSubListener.waitForNotification ();
    generalListener.waitForNotification ();
    
    assertTrue (generalListener.event.matches ().contains (insecureSub));
    assertFalse (generalListener.event.matches ().contains (secureSub));
    
    assertInsecureMatch (generalListener, insecureSub);
    assertInsecureMatch (generalListener, semiSecureSub);
    assertInsecureMatch (generalListener, multiSub);
    
    secureSubListener.reset ();
    insecureSubListener.reset ();
    semiSecureSubListener.reset ();
    multiSubListener.reset ();
    generalListener.reset ();
    
    // send insecure with keys
    elvin.send (ntfn, secureNtfnKeys);
    
    secureSubListener.waitForNotification ();
    insecureSubListener.waitForNotification ();
    semiSecureSubListener.waitForNotification ();
    multiSubListener.waitForNotification ();
    generalListener.waitForNotification ();
    
    assertSecureMatch (generalListener, secureSub);
    assertInsecureMatch (generalListener, insecureSub);
    assertSecureMatch (generalListener, semiSecureSub);
    assertInsecureMatch (generalListener, multiSub);
    
    secureSubListener.reset ();
    insecureSubListener.reset ();
    semiSecureSubListener.reset ();
    multiSubListener.reset ();
    generalListener.reset ();
    
    // send secure
    elvin.send (ntfn, secureNtfnKeys);
    
    secureSubListener.waitForNotification ();
    semiSecureSubListener.waitForNotification ();
    generalListener.waitForNotification ();
    
    assertSecureMatch (generalListener, secureSub);
    assertSecureMatch (generalListener, semiSecureSub);
    
    elvin.close ();
  }

  private static void assertInsecureMatch (TestGeneralNtfnListener listener,
                                           Subscription subscription)
  {
    assertTrue (listener.event.insecureMatches ().contains (subscription));
  }
  
  private static void assertSecureMatch (TestGeneralNtfnListener listener,
                                         Subscription subscription)
  {
    assertTrue (listener.event.secureMatches ().contains (subscription));
  }

  @Test
  public void connectionOptions ()
    throws Exception
  {
    createServer ();
    
    ConnectionOptions options = new ConnectionOptions ();
    
    options.set ("Subscription.Max-Length", Integer.MAX_VALUE);
    options.set ("Subscription.Max-Count", Integer.MAX_VALUE);
    options.set ("Totally.Bogus", "Hello!");
    
    try
    {
      new Elvin (new ElvinURI (ELVIN_URI), options, EMPTY_KEYS, EMPTY_KEYS);
      
      fail ("Failed to reject bogus options");
    } catch (ConnectionOptionsException ex)
    {
      // ok

      // System.out.println ("options = " + ex.getMessage ());
      assertTrue (ex.rejectedOptions.keySet ().equals
                   (set ("Subscription.Max-Length",
                         "Subscription.Max-Count",
                         "Totally.Bogus")));
    }
  }

  @Test
  public void protocolVersion ()
    throws Exception
  {
    createServer ();
    
    ElvinURI uri = new ElvinURI (ELVIN_URI);
    uri.versionMinor++;
    
    try
    {
      new Elvin (new ElvinURI (uri));
      
      fail ("Failed to reject bogus version");
    } catch (IOException ex)
    {
      // ok

      // System.out.println ("error = " + ex.getMessage ());
    }
  }
  
  @Test
  public void invalidSubscription ()
    throws Exception
  {
    createServer ();
    
    Elvin elvin = new Elvin (ELVIN_URI);
    
    try
    {
      elvin.subscribe ("require (foo");
      
      fail ();
    } catch (InvalidSubscriptionException ex)
    {
      // ok
      assertEquals ("require (foo", ex.expression);
      assertEquals (SYNTAX_ERROR, ex.reason);
    }
    
    try
    {
      elvin.subscribe ("1 == 1");
      
      fail ();
    } catch (InvalidSubscriptionException ex)
    {
      // ok
      assertEquals ("1 == 1", ex.expression);
      assertEquals (TRIVIAL_EXPRESSION, ex.reason);
    }
  }
  
  static void checkSecureSendReceive (Elvin client,
                                      Subscription secureSub,
                                      Subscription insecureSub)
    throws IOException, InterruptedException
  {
    TestNtfnListener secureListener = new TestNtfnListener (secureSub);
    
    Notification ntfn = new Notification ();
    ntfn.set ("From-Alice", 1);
    
    secureListener.waitForNotification (client, ntfn);
    assertNotNull (secureListener.event);
    assertTrue (secureListener.event.secure);

    TestNtfnListener insecureListener = new TestNtfnListener (insecureSub);
    insecureListener.waitForNotification (client, ntfn);
    assertNull (insecureListener.event);
  }
  
  @Ignore
  @Test
  public void thrashTest ()
    throws Exception
  {
    for (int  i = 0; i < 1000; i++)
    {
      System.out.println ("***** " + i);
      
      modifySubInNotifyEvent ();
    }
  }
  
  @Test
  public void serverShutdown ()
    throws Exception
  {
    createServer ();
    Elvin client = new Elvin (ELVIN_URI);
    
    client.subscribe ("require (test)");
    
    TestCloseListener listener = new TestCloseListener ();
    
    client.addCloseListener (listener);
    
    synchronized (listener)
    {
      server.close ();

      listener.wait (5000);
    }
    
    assertFalse (client.isOpen ());
    
    try
    {
      client.subscribe ("require (hello)");
      
      fail ("Client did not reject attempt to use closed connection");
    } catch (IOException ex)
    {
      // ok
    }
    
    client.close ();
  }
  
  @Test
  public void closeListener ()
    throws Exception
  {
    createServer ();
    Elvin client = new Elvin (ELVIN_URI);
    
    // client shuts down
    TestCloseListener listener = new TestCloseListener ();
    
    client.addCloseListener (listener);
    
    client.close ();
    
    // close event must be fired before close () returns
    assertNotNull (listener.event);
    assertEquals (REASON_CLIENT_SHUTDOWN, listener.event.reason);
    
    // server shuts down cleanly
    client = new Elvin (ELVIN_URI);
    listener = new TestCloseListener ();
    
    client.addCloseListener (listener);
    
    synchronized (listener)
    {
      server.close ();
      
      listener.wait (5000);
    }
    
    assertNotNull ("No event fired", listener.event);
    assertEquals (REASON_ROUTER_SHUTDOWN, listener.event.reason);
    
    // check we don't get another close event
    listener.event = null;
    client.close ();
    assertNull (listener.event);
    
    // todo test close () in callback
    
    // simulate server crash
//    createServer ();
//    
//    client = new Elvin (ELVIN_URI);
//    listener = new TestConnectionListener ();
//    
//    client.setLivenessTimeout (1000);
//    
//    client.addConnectionListener (listener);
//    
//    server.testSimulateHang ();
//    
//    Thread.sleep (2000);
//    assertNotNull (listener.event);
//    assertEquals (ConnectionEvent.REASON_ROUTER_STOPPED_RESPONDING,
//                  listener.event.reason);
    
  }
  
  private static void wait (Object sem)
    throws InterruptedException
  {
    long waitStart = currentTimeMillis ();
    
    sem.wait (10000);
    
    if (currentTimeMillis () - waitStart >= 10000)
      fail ("Timed out waiting for response");
  }
  
  static class TestCloseListener implements CloseListener
  {
    public CloseEvent event;

    public synchronized void connectionClosed (CloseEvent e)
    {
      event = e;
      
      notifyAll ();
    }
  }
  
  static class TestNtfnListener implements NotificationListener
  {
    public NotificationEvent event;

    public TestNtfnListener (Subscription sub)
    {
      sub.addListener (this);
    }

    public synchronized void notificationReceived (NotificationEvent e)
    {
      event = e;
      
      notifyAll ();
    }

    public synchronized void waitForNotification (Elvin client,
                                                  Notification ntfn)
      throws InterruptedException, IOException
    {
      client.send (ntfn, REQUIRE_SECURE_DELIVERY);
      
      wait (2000);
    }
    
    public void reset ()
    {
      event = null;
    }
    
    public synchronized void waitForNotification ()
      throws InterruptedException
    {
      receiveNotification ();
      
      if (event == null)
        fail ("No notification received");
    }
    
    public synchronized void waitForNoNotification ()
      throws InterruptedException
    {
      receiveNotification ();
      
      if (event != null)
        fail ("Notification received");
    }

    private synchronized void receiveNotification ()
      throws InterruptedException
    {
      if (event == null)
        wait (2000);
    }
  }
  
  static class TestGeneralNtfnListener implements GeneralNotificationListener
  {
    public GeneralNotificationEvent event;

    public TestGeneralNtfnListener (Elvin elvin)
    {
      elvin.addNotificationListener (this);
    }

    public synchronized void notificationReceived (GeneralNotificationEvent e)
    {
      event = e;  
      
      notifyAll ();
    }
    
    public void reset ()
    {
      event = null;
    }
    
    public synchronized void waitForNotification ()
      throws InterruptedException
    {
      if (event == null)
      {
        long now = currentTimeMillis ();
        
        wait (2000);
        
        if (currentTimeMillis () - now >= 2000)
          fail ("No notification received");
      }
    }
  }
}
