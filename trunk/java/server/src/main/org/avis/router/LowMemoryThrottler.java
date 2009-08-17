package org.avis.router;

import java.util.Iterator;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;

import static org.avis.io.Net.idFor;
import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.warn;
import static org.avis.router.StatsFilter.readBytesThroughput;
import static org.avis.router.StatsFilter.updateThroughput;

public class LowMemoryThrottler extends IoFilterAdapter
{
  // TODO these should be based in max frame size
  private static final long HIGH_WATER = 4 * 1024 * 1024;
  private static final long LOW_WATER = 4 * 1024 * 1024;

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
    checkForLowMemory ();
    
    super.filterWrite (nextFilter, session, writeRequest);
  }
  
  @Override
  public void messageReceived (NextFilter nextFilter,
                               IoSession session, Object message)
    throws Exception
  {
    checkForLowMemory ();
    
    super.messageReceived (nextFilter, session, message);
  }

  private void checkForLowMemory ()
  {
    if (getRuntime ().freeMemory () < HIGH_WATER)
    {
      System.gc ();
      
      if (getRuntime ().freeMemory () < HIGH_WATER)
      {
        synchronized (this)
        {
          if (!inLowMemoryState ())
            enterLowMemoryState ();
        }
      }
    }
  }

  private void enterLowMemoryState ()
  {
    warn ("Entering low memory state: throttling clients: " +
          formatBytes (getRuntime ().freeMemory ()) + " < " + 
          formatBytes (LOW_WATER) + " bytes free", this);
    
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
    for (IoSession session : ioManager.sessions ())
      session.suspendRead ();
  }
  
  protected void unthrottle ()
  {
    diagnostic ("Leaving low memory state: unthrottling clients: " +
                formatBytes (getRuntime ().freeMemory ()) + " > " + 
                formatBytes (HIGH_WATER) + " bytes free", this);

    synchronized (this)
    {
      poller = null;
      
      for (IoSession session : ioManager.sessions ())
        session.resumeRead ();      
    }
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
  
  static String formatBytes (long bytes)
  {
    return String.format ("%,d", bytes);
  }

  /**
   * While in low memory condition, loops though sessions periodically
   * enabling one per second to allow draining to occur.
   * 
   * @author Matthew Phillips
   */
  private class PollingThread extends Thread
  {
    public PollingThread ()
    {
      super ("Low memory poller");
      
      setPriority (MAX_PRIORITY);
    }

    @Override
    public void run ()
    {
      killASpammyClient ();
      
      Iterator<IoSession> sessionIter = ioManager.sessions ().iterator ();
      IoSession lastSession = null;
      
      try
      {
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
            diagnostic ("Low memory manager: " + 
                        formatBytes (getRuntime ().freeMemory ()) + 
                        " free memory", this);
          }
          
          synchronized (this)
          {
            wait (1000);
          }
        }
      } catch (InterruptedException ex)
      {
        interrupt ();
      } finally
      {
        unthrottle ();
      }
    }

    /**
     * Kil the first client with more than 50Kb/s incoming.
     */
    private void killASpammyClient ()
    {
      long maxThrougput = 50 * 1024;
      IoSession spammyClient = null;
      
      for (IoSession session : ioManager.sessions ())
      {
        updateThroughput (session);
        
        long throughput = (long)readBytesThroughput (session);
        
        if (throughput > maxThrougput)
        {
          maxThrougput = throughput;          
          spammyClient = session;
        }
      }
      
      if (spammyClient != null)
      {
        diagnostic ("Low memory manager: killing spammy client " + 
                    idFor (spammyClient) + " with " + 
                    formatBytes (maxThrougput) + " throughput", this);
        
        spammyClient.close (true);
      }
    }
  }
}
