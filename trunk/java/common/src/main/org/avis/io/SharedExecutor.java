package org.avis.io;

import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newScheduledThreadPool;

/**
 * Manages a single shared ScheduledExecutorService.
 * 
 * @author Matthew Phillips
 */
public final class SharedExecutor
{
  protected static int shareCount = 0;
  protected static ScheduledExecutorService sharedExecutor = null;
  
  private SharedExecutor ()
  {
    // zip
  }
  
  /**
   * Acquire a reference to the shared executor.
   */
  public static ScheduledExecutorService acquire ()
  {
    synchronized (SharedExecutor.class)
    {
      if (shareCount++ == 0)
        sharedExecutor = newScheduledThreadPool (1);

      return sharedExecutor;
    }
  }

  /**
   * Release the shared exectutor.
   * 
   * @param executor The executor. If this is not the shared instance,
   *                nothing is done.
   */
  public static boolean release (ScheduledExecutorService executor)
  {
    synchronized (SharedExecutor.class)
    {
      if (sharedExecutor != null && executor == sharedExecutor)
      {
        if (shareCount == 0)
          throw new IllegalStateException ("Too many release calls");

        if (--shareCount == 0)
        {
          sharedExecutor.shutdown ();
          sharedExecutor = null;
        }
     
        return true;
      }
    }
    
    return false;
  }

  public static boolean sharedExecutorDisposed ()
  {
    synchronized (SharedExecutor.class)
    {
      return shareCount == 0 && sharedExecutor == null;
    }
  }
}
