package org.avis.net.client;

import java.io.IOException;

import org.avis.common.Notification;
import org.avis.net.server.Server;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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
  
  @Test
  public void modifySubInNotifyEvent ()
    throws Exception
  {
    server = new Server (29170);
    
    final Elvin client = new Elvin ("elvin://localhost:29170");
    
    final Subscription sub = client.subscribe ("require(test)");
    
    assertTrue (client.hasSubscription (sub));
    
    sub.addNotificationListener (new NotificationListener ()
    {
      public void notificationReceived (NotificationEvent e)
      {
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
      long waitStart = currentTimeMillis ();
      
      sub.wait (10000);
      
      if (currentTimeMillis () - waitStart >= 10000)
        fail ("Timed out waiting for unsubscribe");
    }
    
    assertFalse (client.hasSubscription (sub));
    
    client.close ();
    server.close ();
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
}
