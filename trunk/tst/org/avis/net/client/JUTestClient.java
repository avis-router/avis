package org.avis.net.client;

import java.io.IOException;

import org.avis.common.Notification;
import org.avis.net.server.Server;

import org.junit.Test;

import static java.lang.System.currentTimeMillis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JUTestClient
{
  @Test
  public void modifySubInNotifyEvent ()
    throws Exception
  {
    Server server = new Server (29170);
    
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
    
    client.send (ntfn);
    
    synchronized (sub)
    {
      long waitStart = currentTimeMillis ();
      
      sub.wait (5000);
      
      if (currentTimeMillis () - waitStart >= 5000)
        fail ("Timed out waiting for unsubscribe");
    }
    
    assertFalse (client.hasSubscription (sub));
    
    client.close ();
    server.close ();
  }
  
  @Test
  public void serverShutdown ()
    throws Exception
  {
    Server server = new Server (29170);
    Elvin client = new Elvin ("elvin://localhost:29170");
    
    client.subscribe ("require (test)");
    
    server.close ();
    
    // todo should listen for event here when supported
    Thread.sleep (2000);
    
    assertFalse (client.isConnected ());
    
    client.close ();
  }
}
