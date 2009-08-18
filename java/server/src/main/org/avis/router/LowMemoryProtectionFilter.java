package org.avis.router;

import java.util.Iterator;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;

import static org.avis.io.Net.idFor;
import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.warn;
import static org.avis.router.StatsFilter.readBytesThroughput;
import static org.avis.router.StatsFilter.updateThroughput;

/**
 * Checks free memory at every message send/receive, and triggers low
 * memory state protection when memory has dropped to a certain level.
 * 
 * In low memory state:
 * <ul>
 * <li>Each time the low memory trigger is hit, the "spammiest" client
 * (if any) gets dropped.
 * <li>All reads from clients are suspended
 * <li>Client reads are resumed in round-robin for a second each until enough
 * memory is available.
 * </ul>
 * 
 * @author Matthew Phillips
 */
public class LowMemoryProtectionFilter extends IoFilterAdapter
{
  protected IoManager ioManager;
  protected Thread poller;
  protected long lowMemoryTrigger;
  protected long lowMemoryUntrigger;

  public LowMemoryProtectionFilter (IoManager ioManager, int maxFrameSize)
  {
    this.ioManager = ioManager;
    this.lowMemoryTrigger = max (maxFrameSize * 2, 4 * 1024 * 1024);
    this.lowMemoryUntrigger = lowMemoryTrigger + (maxFrameSize / 2);
    
    if (getRuntime ().freeMemory () < lowMemoryTrigger)
    {
      warn 
        ("Low memory trigger has been reached before router has started: " +
         "disabling low memory crash protection. Consider raising the VM's" +
         "maximum memory allocation to avoid this.", this);
      
      lowMemoryTrigger = Long.MAX_VALUE;
    }
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
    if (!inLowMemoryState () && getRuntime ().freeMemory () < lowMemoryTrigger)
    {
      System.gc ();

      synchronized (this)
      {
        long freeMemory = getRuntime ().freeMemory ();
        
        if (freeMemory < lowMemoryTrigger && !inLowMemoryState ())
        {
          warn ("Entering low memory state: throttling clients: " +
                formatBytes (freeMemory) + " < " + 
                formatBytes (lowMemoryTrigger) + " bytes free", this);        
  
          enterLowMemoryState ();
        }
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
    for (IoSession session : ioManager.sessions ())
      session.suspendRead ();
  }
  
  protected void unthrottle ()
  {
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
        while (getRuntime ().freeMemory () < lowMemoryUntrigger)
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
          
          sleep (1000);
        }
      } catch (InterruptedException ex)
      {
        interrupt ();
      } finally
      {
        diagnostic ("Leaving low memory state: unthrottling clients: " +
                    formatBytes (getRuntime ().freeMemory ()) + " > " + 
                    formatBytes (lowMemoryUntrigger) + " bytes free", this);
        
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
