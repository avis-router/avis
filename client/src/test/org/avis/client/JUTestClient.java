package org.avis.client;

import java.io.IOException;

import org.avis.client.ConnectionOptions;
import org.avis.client.ConnectionOptionsException;
import org.avis.client.Elvin;
import org.avis.client.Notification;
import org.avis.client.NotificationEvent;
import org.avis.client.NotificationListener;
import org.avis.client.Subscription;
import org.avis.common.ElvinURI;
import org.avis.security.Key;
import org.avis.security.Keys;
import org.avis.server.Server;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dsto.dfc.logging.Log;

import static java.lang.System.currentTimeMillis;

import static org.avis.client.Notification.notification;
import static org.avis.client.SecureMode.ALLOW_INSECURE_DELIVERY;
import static org.avis.client.SecureMode.REQUIRE_SECURE_DELIVERY;
import static org.avis.security.KeyScheme.SHA1_PRODUCER;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.util.Collections.set;

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
    Log.setEnabled (Log.TRACE, false);
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
    
    sub.addNotificationListener (new NotificationListener ()
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
    
    sub.addNotificationListener (new NotificationListener ()
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
                                  REQUIRE_SECURE_DELIVERY, bobSubKeys);
    
    eveSub = eveClient.subscribe ("require (From-Alice)");
    
    checkSecureSendReceive (aliceClient, bobSub, eveSub);
    
    aliceClient.close ();
    bobClient.close ();
    eveClient.close ();
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
    
    sub.addNotificationListener (new NotificationListener ()
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
      elvin.subscribe ("require (test)", REQUIRE_SECURE_DELIVERY, secureSubKeys);
    Subscription semiSecureSub =
      elvin.subscribe ("require (test)", ALLOW_INSECURE_DELIVERY, secureSubKeys);
    Subscription insecureSub = elvin.subscribe ("require (test)");
    Subscription multiSub =
      elvin.subscribe ("require (test) || require (frob)");
    
    TestNtfnListener secureSubListener = new TestNtfnListener (secureSub);
    TestNtfnListener semiSecureSubListener = new TestNtfnListener (semiSecureSub);
    TestNtfnListener insecureSubListener = new TestNtfnListener (insecureSub);
    TestNtfnListener multiSubListener = new TestNtfnListener (multiSub);
    TestGeneralNtfnListener generalListener = new TestGeneralNtfnListener (elvin);
    
    Notification ntfn = notification ("test", 1);
    
    // send insecure
    elvin.send (ntfn);
    
    insecureSubListener.waitForNotification ();
    semiSecureSubListener.waitForNotification ();
    multiSubListener.waitForNotification ();
    generalListener.waitForNotification ();
    
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
    
    server.close ();
    
    // todo should listen for event here when supported
    Thread.sleep (2000);
    
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
  
  private static void wait (Object sem)
    throws InterruptedException
  {
    long waitStart = currentTimeMillis ();
    
    sem.wait (10000);
    
    if (currentTimeMillis () - waitStart >= 10000)
      fail ("Timed out waiting for response");
  }
  
  static class TestNtfnListener implements NotificationListener
  {
    public NotificationEvent event;

    public TestNtfnListener (Subscription sub)
    {
      sub.addNotificationListener (this);
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
      if (event == null)
      {
        long now = currentTimeMillis ();
        
        wait (2000);
        
        if (currentTimeMillis () - now >= 2000)
          fail ("No notification received");
      }
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
