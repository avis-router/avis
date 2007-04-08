package org.avis.net.client;

import java.io.IOException;

import org.avis.common.Notification;
import org.avis.net.common.ConnectionOptions;
import org.avis.net.common.ElvinURI;
import org.avis.net.security.Key;
import org.avis.net.security.Keys;
import org.avis.net.server.Server;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.avis.net.security.KeyScheme.SHA1_PRODUCER;
import static org.avis.net.security.Keys.EMPTY_KEYS;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JUTestClient
{
  private Server server;
  private LogFailTester logTester;

  @Before
  public void setup ()
  {
    logTester = new LogFailTester ();
  }
  
  @After
  public void cleanup ()
  {
    if (server != null)
      server.close ();
    
    logTester.dispose ();
  }
  
  /**
   * Test that client handles changing subs in a notification callback.
   */
  @Test
  public void modifySubInNotifyEvent ()
    throws Exception
  {
    server = new Server (29170);
    
    final Elvin client = new Elvin ("elvin://localhost:29170");
    
    final Subscription sub = client.subscribe ("require (test)");
    
    assertTrue (client.hasSubscription (sub));
    
    sub.addNotificationListener (new NotificationListener ()
    {
      public void notificationReceived (NotificationEvent e)
      {
        assertFalse (e.secure);
        
        try
        {
          client.unsubscribe (sub);
          
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
    ntfn.put ("test", 1);
    ntfn.put ("payload", "test 1");
    
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
    server = new Server (29170);
    
    final Elvin client = new Elvin ("elvin://localhost:29170");
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
            ntfn.put ("test", 1);
            ntfn.put ("payload", "test 2");
            
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
    ntfn.put ("test", 1);
    ntfn.put ("payload", "test 1");
    
    synchronized (sub)
    {
      client.send (ntfn);
      
      wait (sub);
    }
    
    client.close ();
    server.close ();
  }

  @Test
  public void securityProducer ()
    throws Exception
  {
    server = new Server (29170);
    ElvinURI uri = new ElvinURI ("localhost", 29170);
    ConnectionOptions options = new ConnectionOptions ();
    
    Key alicePrivate = new Key ("alice private");

    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_PRODUCER, alicePrivate);
    
    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_PRODUCER, alicePrivate.publicKeyFor (SHA1_PRODUCER));
    
    Elvin aliceClient = new Elvin (uri, options, aliceNtfnKeys, EMPTY_KEYS);
    Elvin bobClient = new Elvin (uri, options, EMPTY_KEYS, bobSubKeys);
    
    Subscription sub = bobClient.subscribe ("require (From-Alice)");
    
    checkSecureSendReceive (aliceClient, sub);
    
    aliceClient.close ();
    bobClient.close ();
    
    // check we can add global keys later for same result
    
    aliceClient = new Elvin (uri);
    bobClient = new Elvin (uri);
    
    aliceClient.setKeys (aliceNtfnKeys, EMPTY_KEYS);
    
    sub = bobClient.subscribe ("require (From-Alice)");
    bobClient.setKeys (EMPTY_KEYS, bobSubKeys);
    
    checkSecureSendReceive (aliceClient, sub);
    
    aliceClient.close ();
    bobClient.close ();
  }

  private static void checkSecureSendReceive (Elvin client,
                                              final Subscription sub)
    throws IOException, InterruptedException
  {
    sub.addNotificationListener (new NotificationListener ()
    {
      public void notificationReceived (NotificationEvent e)
      {
        assertTrue (e.secure);
        
        synchronized (sub)
        {
          sub.notifyAll ();
        }
      }
    });
    
    Notification ntfn = new Notification ();
    ntfn.put ("From-Alice", 1);
    
    synchronized (sub)
    {
      client.sendSecure (ntfn);
      
      wait (sub);
    }
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
    server = new Server (29170);
    Elvin client = new Elvin ("elvin://localhost:29170");
    
    client.subscribe ("require (test)");
    
    server.close ();
    
    // todo should listen for event here when supported
    Thread.sleep (2000);
    
    assertFalse (client.isConnected ());
    
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
}
