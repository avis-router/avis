package org.avis.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import java.lang.Thread.UncaughtExceptionHandler;

import static java.util.concurrent.Executors.newScheduledThreadPool;

import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.warn;

/**
 * A single-threaded callback scheduler, which ensures callbacks are
 * executed sequentially.
 * 
 * @author Matthew Phillips
 */
class Callbacks
{
  protected List<Runnable> callbacks;
  protected ScheduledExecutorService executor;
  protected Object callbackMutex;
  protected CallbackRunner callbackRunner;
  protected Future<?> callbackRunnerFuture;
  
  /**
   * Create a new instance.
   * 
   * @param callbackMutex The mutex that is acquired before executing
   *                a callback.
   */
  public Callbacks (Object callbackMutex)
  {
    this.callbackMutex = callbackMutex;
    this.executor = newScheduledThreadPool (1, THREAD_FACTORY);
    this.callbacks = new ArrayList<Runnable> ();
    this.callbackRunner = new CallbackRunner ();
  }
  
  /**
   * Flush callbacks and shutdown scheduler.
   */
  public void shutdown ()
  {
    synchronized (callbackMutex)
    {
      flush ();
      
      executor.shutdown ();
      
      executor = null;
      callbacks = null;
    }
  }
  
  public ScheduledExecutorService executor ()
  {
    return executor;
  }
  
  /**
   * Queue a callback.
   */
  public void queue (Runnable runnable)
  {
    synchronized (this)
    {
      callbacks.add (runnable);
      
      if (callbackRunnerFuture == null)
        callbackRunnerFuture = executor.submit (callbackRunner);
    }
  }
  
  public void flush ()
  {
    synchronized (callbackMutex)
    {
      runCallbacks ();
    }
  }
  
  protected void runCallbacks ()
  {
    if (callbackRunnerFuture != null)
    {
      callbackRunnerFuture.cancel (false);
      callbackRunnerFuture = null;
    }
    
    if (callbacks.size () > 0)
    {
      for (Runnable callback : callbacks)
      {
        try
        {
          callback.run ();
        } catch (RuntimeException ex)
        {
          alarm ("Unhandled exception in callback", this, ex);
        }
      }
      
      callbacks.clear ();
    }
  }

  class CallbackRunner implements Runnable
  {
    public void run ()
    {
      synchronized (callbackMutex)
      {
        runCallbacks ();
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
