package org.avis.client;

import java.io.IOException;

import static org.junit.Assert.fail;

import static org.avis.client.SecureMode.ALLOW_INSECURE_DELIVERY;
import static org.avis.client.SecureMode.REQUIRE_SECURE_DELIVERY;

public class TestNtfnListener implements NotificationListener
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

  /**
   * Send notification securely and wait for one to come back.
   */
  public synchronized void waitForSecureNotification (Elvin client,
                                                      Notification ntfn)
    throws InterruptedException, IOException
  {
    waitForNotification (client, ntfn, REQUIRE_SECURE_DELIVERY, 2000);
  }
  
  public synchronized void waitForNotification (Elvin client,
                                                Notification ntfn,
                                                long timeout)
    throws InterruptedException, IOException
  {
    waitForNotification (client, ntfn, ALLOW_INSECURE_DELIVERY, timeout);
  }
  
  /**
   * Send notification securely and wait for one to come back.
   */
  public synchronized void waitForNotification (Elvin client,
                                                Notification ntfn,
                                                SecureMode secureMode,
                                                long timeout)
    throws InterruptedException, IOException
  {
    client.send (ntfn, secureMode);
    
    wait (timeout);
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