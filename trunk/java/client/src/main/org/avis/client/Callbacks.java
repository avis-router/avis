package org.avis.client;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import java.lang.Thread.UncaughtExceptionHandler;

import org.avis.common.RuntimeInterruptedException;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newScheduledThreadPool;

import static org.avis.logging.Log.warn;

/**
 * A single-threaded callback scheduler, which ensures callbacks are
 * executed sequentially.
 * 
 * @author Matthew Phillips
 */
class Callbacks
{
  protected int callbackCount;
  protected ScheduledExecutorService callbackExecutor;
  protected Object callbackMutex;
  
  /**
   * Create a new instance.
   * 
   * @param callbackMutex The mutex that is acquired before executing
   *                a callback.
   */
  public Callbacks (Object callbackMutex)
  {
    this.callbackMutex = callbackMutex;
    this.callbackExecutor = newScheduledThreadPool (1, THREAD_FACTORY);
  }
  
  /**
   * Flush callbacks and shutdown scheduler.
   */
  public void shutdown ()
  {
    flush ();
    
    callbackExecutor.shutdown ();
  }
  
  public ScheduledExecutorService executor ()
  {
    return callbackExecutor;
  }
  
  /**
   * Queue a callback.
   */
  public void queue (Runnable runnable)
  {
    synchronized (this)
    {
      callbackExecutor.execute (new Callback (runnable));
     
      callbackCount++;
    }
  }
  
  /**
   * Wait for up to 10 seconds for any queued callbacks to be called.
   */
  public void flush ()
  {
    if (!(currentThread () instanceof CallbackThread))
    {
      synchronized (this)
      {
        if (callbackCount > 0)
        {
          if (!waitForNotify (this, 10000))
            warn ("Callbacks took too long to flush", this, new Error ());
        }
      }
    }
  }
  
  protected Object mutex ()
  {
    return this;
  }

  class Callback implements Runnable
  {
    private Runnable runnable;

    public Callback (Runnable runnable)
    {
      this.runnable = runnable;
    }

    public final void run ()
    {
      try
      {
        synchronized (callbackMutex)
        {
          runnable.run ();
        }
      } finally
      {
        synchronized (mutex ())
        {
          if (callbackCount == 0)
            throw new IllegalStateException ("Too many finished callbacks");
          
          if (--callbackCount == 0)
            mutex ().notifyAll ();
        }
      }
    }
  }

  private static boolean waitForNotify (Object object, int maxWait)
  {
    synchronized (object)
    {
      try
      {
        long start = currentTimeMillis ();
        
        object.wait (maxWait);
        
        return currentTimeMillis () - start < maxWait;
      } catch (InterruptedException ex)
      {
        currentThread ().interrupt ();
        
        throw new RuntimeInterruptedException (ex);
      } 
    }
  }
  
  private static final ThreadFactory THREAD_FACTORY =
    new ThreadFactory ()
    {
      public Thread newThread (Runnable target)
      {
        return new CallbackThread (target);
      }
    };
    
  /**
   * The thread used for all callbacks in the callbackExecutor.
   */
  private static class CallbackThread
    extends Thread implements UncaughtExceptionHandler
  {
    private static final AtomicInteger counter = new AtomicInteger ();
    
    public CallbackThread (Runnable target)
    {
      super (target, "Elvin callback thread " + counter.getAndIncrement ());
      
      setUncaughtExceptionHandler (this);
    }
    
    public void uncaughtException (Thread t, Throwable ex)
    {
      warn ("Uncaught exception in Elvin callback", Elvin.class, ex);
    }
  }
}
