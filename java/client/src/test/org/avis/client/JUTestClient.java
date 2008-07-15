package org.avis.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java.io.IOException;

import org.apache.mina.transport.socket.SocketSessionConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.avis.common.ElvinURI;
import org.avis.logging.Log;
import org.avis.router.Router;
import org.avis.security.Key;
import org.avis.security.Keys;
import org.avis.util.LogFailTester;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;

import static org.avis.client.CloseEvent.REASON_CLIENT_SHUTDOWN;
import static org.avis.client.CloseEvent.REASON_ROUTER_SHUTDOWN;
import static org.avis.client.CloseEvent.REASON_ROUTER_STOPPED_RESPONDING;
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

  private static final long WAIT_TIMEOUT = 10000;
  
  private Router server;
  private LogFailTester logTester;
  private int runtime = 6000;
  
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
    server = new Router (29170);
  }
  
  /**
   * Test case where no server is running.
   */
  @Test
  public void noServerRunning ()
    throws Exception
  {
    try
    {
      Elvin client = new Elvin (ELVIN_URI);
      
      client.close ();
      
      fail ();
    } catch (IOException ex)
    {
      // ok
    }
  }

  /**
   * Basic connect test.
   */
  @Test
  public void connect ()
    throws Exception
  {
    createServer ();
    
    ElvinOptions options = new ElvinOptions ();
    
    options.connectionOptions.set ("Packet.Max-Length", 8192);
    
    Elvin client = new Elvin (ELVIN_URI, options);
    
    assertTrue 
      (options.connectionOptions.getString 
         ("Vendor-Identification").startsWith ("Avis"));
    
    client.close ();
    server.close ();
  }
  
  /**
   * Test that client handles changing subs in a notification callback.
   */
  @Test
  public void modifySubInNotify ()
    throws Exception
  {
    createServer ();
    
    final Elvin client = new Elvin (ELVIN_URI);
    
    final Subscription sub = client.subscribe ("require (test)");
    
    assertTrue (client.hasSubscription (sub));
    
    ModifySubListener listener = new ModifySubListener (sub);
    
    Notification ntfn = new Notification ();
    ntfn.set ("test", 1);
    ntfn.set ("payload", "test 1");
    
    client.send (ntfn);
    
    listener.waitForNotification ();
    
    assertFalse (client.hasSubscription (sub));
    
    client.close ();
    server.close ();
  }
  
  private final class ModifySubListener extends TestNtfnListener
  {
    private final Subscription sub;

    public ModifySubListener (Subscription sub)
    {
      super (sub);
      
      this.sub = sub;
    }

    @Override
    public synchronized void notificationReceived (NotificationEvent e)
    {
      if (event != null)
        throw new IllegalStateException ("Notification overflow");

      assertFalse (e.secure);

      try
      {
        sub.remove ();

        // check is not active and can be removed again with no effect
        assertFalse (sub.isActive ());
        
        sub.remove ();

        super.notificationReceived (e);
      } catch (IOException ex)
      {
        throw new RuntimeException (ex);
      }
    }
  }
  
  /**
   * Test that client handles notifying in a notification callback.
   */
  @Test
  public void notifyInNotify ()
    throws Exception
  {
    createServer ();
    
    Elvin client = new Elvin (ELVIN_URI);
    
    final Subscription sub = client.subscribe ("require (Test)");
    
    sub.addListener (new NotificationListener ()
    {
      public void notificationReceived (NotificationEvent e)
      {
        try
        {
          if (e.notification.getInt ("Message-Number") == 1)
          {
            Notification ntfn = new Notification ();
            ntfn.set ("Test", 1);
            ntfn.set ("Message-Number", 2);
            
            e.subscription.elvin ().send (ntfn);
          } else if (e.notification.getInt ("Message-Number") == 2)
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
    ntfn.set ("Test", 1);
    ntfn.set ("Message-Number", 1);
    
    synchronized (sub)
    {
      client.send (ntfn);
      
      waitOn (sub);
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
    
    Key alicePrivate = new Key ("alice private");

    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_PRODUCER, alicePrivate);
    
    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_PRODUCER, alicePrivate.publicKeyFor (SHA1_PRODUCER));
    
    // subscribe with global keys
    Elvin aliceClient = new Elvin (uri, aliceNtfnKeys, EMPTY_KEYS);
    Elvin bobClient = new Elvin (uri, EMPTY_KEYS, bobSubKeys);
    Elvin eveClient = new Elvin (uri);
    
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
    aliceClient = new Elvin (uri, aliceNtfnKeys, EMPTY_KEYS);
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
    aliceClient = new Elvin (uri, aliceNtfnKeys, EMPTY_KEYS);
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
    
    Elvin client = new Elvin (ELVIN_URI);
    Subscription sub = client.subscribe ("require (test)");
    
    TestNtfnListener listener = new TestNtfnListener (sub);
    
    checkReceive (client, listener, "test");
    checkNotReceive (client, listener, "not_test");

    // change subscription
    sub.setSubscriptionExpr ("require (test2)");
    
    checkReceive (client, listener, "test2");
    checkNotReceive (client, listener, "test");
    
    long id = sub.id;
    
    assertTrue (sub.isActive ());
    
    sub.remove ();
    
    assertFalse (sub.isActive ());
    
    assertNull (client.subscriptions.get (id));
    
    checkNotReceive (client, listener, "test2");
    
    client.close ();
  }

  /**
   * Check that client receives a notification with a required field.
   */
  private static void checkReceive (Elvin client, TestNtfnListener listener,
                                    String requiredField)
    throws IOException, InterruptedException
  {
    Notification ntfn = new Notification ();
    
    ntfn.set (requiredField, 1);
    
    listener.waitForNotification (client, ntfn, WAIT_TIMEOUT);
    
    listener.reset ();
  }
  
  /**
   * Check that client does not receive a notification with a required
   * field.
   */
  private static void checkNotReceive (Elvin client, TestNtfnListener listener,
                                       String notRequiredField)
    throws IOException, InterruptedException
  {
    Notification ntfn = new Notification ();
    ntfn.set (notRequiredField, 1);
    
    client.send (ntfn);
    
    listener.waitForNoNotification ();
    
    listener.reset ();
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
    
    assertTrue (generalListener.event.matches.contains (insecureSub));
    assertFalse (generalListener.event.matches.contains (secureSub));
    
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
    assertTrue (listener.event.insecureMatches.contains (subscription));
  }
  
  private static void assertSecureMatch (TestGeneralNtfnListener listener,
                                         Subscription subscription)
  {
    assertTrue (listener.event.secureMatches.contains (subscription));
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
      new Elvin (ELVIN_URI, options);
      
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
    
    // check TCP.Send-Immediately on client side
    options = new ConnectionOptions ();
    
    options.set ("TCP.Send-Immediately", true);
    
    Elvin client = new Elvin (ELVIN_URI, options);
    
    assertTrue 
      (((SocketSessionConfig)client.connection.getConfig ()).isTcpNoDelay ());
    
    client.close ();
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
    
    secureListener.waitForSecureNotification (client, ntfn);
    assertNotNull (secureListener.event);
    assertTrue (secureListener.event.secure);

    TestNtfnListener insecureListener = new TestNtfnListener (insecureSub);
    insecureListener.waitForSecureNotification (client, ntfn);
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
      
      setup ();
      
      runtime = 30000;
      
      //Log.enableLogging (Log.TRACE, true);
      //firehose ();
      multiThread ();

      cleanup ();
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
    } catch (NotConnectedException ex)
    {
      // ok
    }
    
    client.close ();
  }
  
  @Test
  public void clientClose ()
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
  }
  
  @Test
  public void serverClose ()
    throws Exception
  {
    createServer ();
    
    // server shuts down cleanly
    Elvin client = new Elvin (ELVIN_URI);
    TestCloseListener listener = new TestCloseListener ();
    
    client.addCloseListener (listener);
    
    synchronized (listener)
    {
      server.close ();
      
      waitOn (listener, 4000);
    }
    
    assertNotNull ("No event fired", listener.event);
    assertEquals (REASON_ROUTER_SHUTDOWN, listener.event.reason);
    
    // check we don't get another close event
    listener.event = null;
    client.close ();
    assertNull (listener.event);
  }
  
  @Test
  public void liveness () 
    throws Exception
  {
    createServer ();

    Elvin client = new Elvin (ELVIN_URI);
    
    TestCloseListener listener = new TestCloseListener ();
    
    client.addCloseListener (listener);

    client.setReceiveTimeout (1000);
    client.setLivenessTimeout (1000);
    
    // check liveness check keeps connection open
    sleep (3000);
    
    assertNull (listener.event == null ? "" : 
                "Unexpected close event " + listener.event.message, 
                listener.event);
    assertTrue (client.isOpen ());
    
    // hang server
    server.testSimulateHang ();
    
    // check liveness detects and closes connection
    synchronized (listener)
    {
      waitOn (listener, 4000);
    }
    
    assertNotNull (listener.event);
    assertEquals (REASON_ROUTER_STOPPED_RESPONDING,
                  listener.event.reason);
    assertFalse (client.isOpen ());
    
    server.testSimulateUnhang ();
  }
  
  /**
   * Test that calling close () in a callback works.
   */
  @Test
  public void closeInCallback ()
    throws Exception
  {
    createServer ();
    final Elvin client = new Elvin (ELVIN_URI);
    
    TestCloseListener closeListener = new TestCloseListener ();
    
    client.addCloseListener (closeListener);
    
    NotificationListener listener = new NotificationListener ()
    {
      public void notificationReceived (NotificationEvent e)
      {
        client.close ();
      } 
    };
    
    client.subscribe ("require (test)").addListener (listener);
    
    Notification ntfn = new Notification ();
    
    ntfn.set ("test", 1);
    
    client.send (ntfn);
      
    closeListener.waitForEvent ();
    
    assertFalse (client.isOpen ());
  }
  
  /**
   * Create a "firehose" thread that fires 100 events per second at a
   * client, see how it reacts when rapidly changing subs/closing.
   */
  @Test
  public void firehose ()
    throws Exception
  {
    createServer ();
    
    Elvin client = new Elvin (ELVIN_URI);
    final Elvin firehoseClient = new Elvin (ELVIN_URI);
    
    // raise timeouts to accomodate the higher loads
    client.setReceiveTimeout (20000);
    firehoseClient.setReceiveTimeout (20000);
    
    Subscription subscription = client.subscribe ("require (test)");
    
    new TestNtfnListener (subscription);
    
    Thread firehose = new Thread ()
    {
      @Override
      public void run ()
      {
        Notification ntfn = new Notification ();
        ntfn.set ("test", 1);
        
        while (!interrupted () && firehoseClient.isOpen ())
        {
          try
          {
            firehoseClient.send (ntfn);
            
            try
            {
              sleep (10);
            } catch (InterruptedException ex)
            {
              interrupt ();
            }
          } catch (IOException ex)
          {
            ex.printStackTrace ();
            interrupt ();
          }
        }
      }
    };
    
//    Log.enableLogging (Log.TRACE, true);
    
    firehose.start ();
    
    long start = currentTimeMillis ();
    
    // pound on it for a while
    
    while (currentTimeMillis () - start < runtime)
    {
      subscription.remove ();
      
      subscription = client.subscribe ("require (test)");
      
      new TestNtfnListener (subscription);
    }
    
    // todo check callbacks do not happen after close
    client.close ();
    
    firehose.interrupt ();
    
    firehose.join (10000);
    
    firehoseClient.close ();
  }
  
  // todo test setKeys and setSubscription
  @Test
  public void multiThread ()
    throws Exception
  {
    createServer ();
    
    Elvin client = new Elvin (ELVIN_URI);
    
    // raise timeouts to accomodate the higher loads
    client.setReceiveTimeout (20000);
    
    List<MultiThreadClientThread> threads = 
      new ArrayList<MultiThreadClientThread> ();
    
    for (int i = 0; i < 16; i++)
      threads.add (new MultiThreadClientThread (client, i));
    
    for (MultiThreadClientThread thread : threads)
      thread.start ();
    
    // pound on it for a while
    sleep (runtime);
    
    // shut down client threads
    for (MultiThreadClientThread thread : threads)
      thread.running = false;

    for (MultiThreadClientThread thread : threads)
    {
      long now = currentTimeMillis ();
      
      thread.join (WAIT_TIMEOUT);
      
      if (currentTimeMillis () - now > WAIT_TIMEOUT)
        fail ("Thread took too long to shut down");
    }
    
    client.close ();
  }
  
  static class MultiThreadClientThread extends Thread
  {
    private Elvin client;
    private Random random;

    public volatile boolean running;
    
    public MultiThreadClientThread (Elvin client, int number)
    {
      this.client = client;
      this.random = new Random (number);
    }
    
    @Override
    public void run ()
    {
      running = true;
      
      try
      {
        client.subscribe ("require (number)");
        
        while (running && !interrupted () && client.isOpen ())
        {
          Subscription sub = 
            client.subscribe ("number <= " + random.nextInt (5));
          
          Notification ntfn = new Notification ();
          ntfn.set ("number", random.nextInt (5));
          
          client.send (ntfn);
          
          sub.remove ();
        }
      } catch (Exception ex)
      {
        if (!interrupted () && ex instanceof IOException)
          Log.alarm ("Error in client thread", this, ex);
        
        interrupt ();
      }
    }
  }
  
  @Test
  public void callbacks ()
    throws Exception
  {
    createServer ();
    
    final Elvin client = new Elvin (ELVIN_URI);
    
    // grab mutex
    synchronized (client.mutex ())
    {
      Subscription sub = client.subscribe ("require (test)");

      client.send (new Notification ("test", 1));

      // subscribe forces callback flush
      
      client.send (new Notification ("test", 1));

      sleep (2000);
      
      sub.remove ();
      
      TestCloseListener listener = new TestCloseListener ();
      
      client.addCloseListener (listener);
      
      // close forces callback flush: should do it in this thread, no blocking
      client.close ();
      
      assertNotNull (listener.event);
    }
  }
  
  static void waitOn (Object lock)
    throws InterruptedException
  {
    waitOn (lock, WAIT_TIMEOUT);
  }
  
  static void waitOn (Object lock, long timeout)
    throws InterruptedException
  {
    long waitStart = currentTimeMillis ();
    
    lock.wait (timeout);
    
    if (currentTimeMillis () - waitStart >= timeout)
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
    
    public synchronized void waitForEvent ()
      throws InterruptedException
    {
      if (event == null)
        waitOn (this);
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
