package org.avis.federation;

import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.Assert.fail;

public class TestUtils
{
  /**
   * Wait up to 10 seconds for a connector to be in connected state.
   */
  public static void waitForConnect (Connector connector)
    throws InterruptedException
  {
    long start = currentTimeMillis ();
    
    while (!connector.isConnected () && currentTimeMillis () - start < 10000)
      sleep (200);
    
    if (!connector.isConnected ())
      fail ("Failed to connect");
  }

  /**
   * Wait up to 10 seconds for a connector to go to waiting for an
   * async connect.
   */
  public static void waitForAsyncConnect (Connector connector)
    throws InterruptedException
  {
    long start = currentTimeMillis ();
    
    while (currentTimeMillis () - start < 10000 && 
           !connector.isWaitingForAsyncConnection ())
    {
      sleep (200);
    }
    
    if (!connector.isWaitingForAsyncConnection ())
      fail ("Failed to go to async connect state");
  }

  public static void waitForSingleLink (final Acceptor acceptor) 
    throws InterruptedException
  {
    waitUpto (8, SECONDS, new Predicate ()
    {
      public boolean satisfied ()
      {
        return acceptor.links.size () == 1;
      }
    });
  }
  
  public static void waitUpto (long duration, TimeUnit unit,
                               Predicate predicate) 
    throws InterruptedException
  {
    long finishAt = currentTimeMillis () + unit.toMillis (duration);
    
    while (!predicate.satisfied () && currentTimeMillis () < finishAt)
      sleep (10);
    
    if (!predicate.satisfied ())
      fail ();
  }
}
