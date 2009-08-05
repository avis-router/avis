package org.avis.router;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

import org.apache.mina.core.session.IoSession;

import static java.lang.Runtime.getRuntime;

public class WeakLowMemoryThrottler extends Thread
{
  private IoManager ioManager;
  private ReferenceQueue<Object> refQueue;
  private SoftReference<Object> ref;

  public WeakLowMemoryThrottler (IoManager ioManager)
  {
    super ("Low memory manager");
    
    setPriority (MAX_PRIORITY);
    
    this.ioManager = ioManager;
    this.refQueue = new ReferenceQueue<Object> ();
    
    initReference ();
    
    start ();
  }

  private void initReference ()
  {
    this.ref = new SoftReference<Object> (new byte [1024], refQueue);
  }
  
  @Override
  public void run ()
  {
    try
    {
      while (!interrupted ())
      {
        refQueue.remove ();
        
        System.out.println ("*** Throttle on low memory");
    
        throttle ();
        
        pollForUnthrottle ();
        
        System.out.println ("*** unthrottle");
        
        unthrottle ();
        
        initReference ();
      }
    } catch (InterruptedException ex)
    {
      // re-interrupt and exit
      interrupt ();
    }
  }

  private void throttle ()
  {
    // TODO suspend new connections
    for (IoSession session : ioManager.sessions ())
      session.suspendRead ();
  }
  
  private void unthrottle ()
  {
    // TODO unsuspend new connections
    for (IoSession session : ioManager.sessions ())
      session.resumeRead ();
  }

  private void pollForUnthrottle () 
    throws InterruptedException
  {
    // TODO think of more intelligent way to re-enable
    while (getRuntime ().freeMemory () < 4 * 1024 * 1024)
    {
      synchronized (this)
      {
        System.gc ();
        wait (1000);
        System.out.println ("free = " + getRuntime ().freeMemory ());
      }
    }
  }
  
  public synchronized void shutdown ()
  {
    interrupt ();
    
    try
    {
      join (5000);
    } catch (InterruptedException ex)
    {
      interrupt ();
    }
  }
}
