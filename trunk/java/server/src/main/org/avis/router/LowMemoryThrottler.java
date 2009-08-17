package org.avis.router;

import java.util.Iterator;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;

import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.warn;

public class LowMemoryThrottler extends IoFilterAdapter
{
  private static final long HIGH_WATER = 4 * 1024 * 1024;
  private static final long LOW_WATER = 4 * 1024 * 1024;
  
  /**
   * While in low memory condition, loops though sessions periodically
   * enabling one per second to allow draining to occur.
   * 
   * @author Matthew Phillips
   */
  private final class PollingThread extends Thread
  {
    public PollingThread ()
    {
      super ("Low memory poller");
      
      setPriority (MAX_PRIORITY);
    }

    @Override
    public void run ()
    {
      Iterator<IoSession> sessionIter = ioManager.sessions ().iterator ();
      IoSession lastSession = null;
      
      try
      {
        // TODO think of more intelligent way to re-enable: use frame size
        while (getRuntime ().freeMemory () < LOW_WATER)
        {
          IoSession session;
          
          if (lastSession != null)
            lastSession.suspendRead ();
          
          if (!sessionIter.hasNext ())
            sessionIter = ioManager.sessions ().iterator ();
          
          if (sessionIter.hasNext ())
          {
            session = sessionIter.next ();
            session.resumeRead ();
            lastSession = session;
          } else
          {
            session = null;
          }
          
          System.gc ();
          
          if (shouldLog (DIAGNOSTIC))
          {
            diagnostic ("Low memory state poller:  " + 
                        getRuntime ().freeMemory () + " free memory", this);
          }
          
          synchronized (this)
          {
            wait (1000);
          }
        }
        
        unthrottle ();        
      } catch (InterruptedException ex)
      {
        interrupt ();
      }
    }
  }

  protected IoManager ioManager;
  protected Thread poller;

  public LowMemoryThrottler (IoManager ioManager)
  {
    this.ioManager = ioManager;
  }
  
  @Override
  public void sessionCreated (NextFilter nextFilter, IoSession session)
    throws Exception
  { 
    super.sessionCreated (nextFilter, session);
    
    synchronized (this)
    {
      if (inLowMemoryState ())
        session.suspendRead ();
    }
  }

  @Override
  public void filterWrite (NextFilter nextFilter, IoSession session,
                           WriteRequest writeRequest)
    throws Exception
  {
    checkMemory ();
    
    super.filterWrite (nextFilter, session, writeRequest);
  }
  
  @Override
  public void messageReceived (NextFilter nextFilter,
                               IoSession session, Object message)
    throws Exception
  {
    checkMemory ();
    
    super.messageReceived (nextFilter, session, message);
  }

  private void checkMemory ()
  {
    if (getRuntime ().freeMemory () < HIGH_WATER)
    {
      synchronized (this)
      {
        if (!inLowMemoryState ())
          enterLowMemoryState ();
      }
    }
  }

  private void enterLowMemoryState ()
  {
    throttle ();
    
    poller = new PollingThread ();
    poller.start ();
  }

  private boolean inLowMemoryState ()
  {
    return poller != null;
  }

  private void throttle ()
  {
    warn ("Entering low memory state: throttling clients: " +
          getRuntime ().freeMemory () + " < " + LOW_WATER + " bytes free", this);
    
    for (IoSession session : ioManager.sessions ())
      session.suspendRead ();
  }
  
  protected synchronized void unthrottle ()
  {
    poller = null;
    
    diagnostic ("Leaving low memory state: unthrottling clients: " +
                getRuntime ().freeMemory () + " > " + HIGH_WATER + 
                " bytes free", this);

    for (IoSession session : ioManager.sessions ())
      session.resumeRead ();
  }
  
  public synchronized void shutdown ()
  {
    if (poller != null)
    {
      synchronized (poller)
      {
        poller.interrupt ();
      }
     
      try
      {
        poller.join (5000);
      } catch (InterruptedException ex)
      {
        currentThread ().interrupt ();
      }
    }
  }
}
