package org.avis.router;

import java.io.IOException;

import java.lang.management.MemoryUsage;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

import org.avis.io.messages.DropWarn;
import org.avis.io.messages.NotifyDeliver;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.management.ManagementFactory.getMemoryMXBean;

import static org.avis.io.Net.idFor;
import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.warn;
import static org.avis.router.RouterOptionSet.sendQueueDropPolicy;
import static org.avis.router.RouterOptionSet.sendQueueMaxLength;
import static org.avis.router.StatsFilter.readBytesThroughput;
import static org.avis.router.StatsFilter.updateThroughput;
import static org.avis.util.Text.formatNumber;

/**
 * Checks free memory and send queue size at every message
 * send/receive.
 * 
 * Triggers low memory state protection when memory has dropped to a
 * certain level.
 * 
 * In low memory state:
 * <ul>
 * <li>Each time the low memory trigger is hit, the "spammiest" client
 * (if any) gets dropped.
 * <li>All reads from clients are suspended
 * <li>Reads are resumed when enough memory is available.
 * </ul>
 * 
 * @author Matthew Phillips
 */
public class LowMemoryProtectionFilter extends IoFilterAdapter
{
  private static final String IN_DROPWARN_STATE = "dropwarn";
  
  protected IoManager ioManager;
  protected Thread poller;
  protected long lowMemoryThreshold;

  public LowMemoryProtectionFilter (IoManager ioManager, 
                                    long lowMemoryThreshold)
  {
    this.ioManager = ioManager;
    this.lowMemoryThreshold = lowMemoryThreshold;
    
    System.gc ();
    
    if (freeMemory () < lowMemoryThreshold)
    {
      warn 
        ("Low memory trigger has been reached before router has started: " +
         "disabling low memory crash protection. Consider raising the VM's " +
         "maximum memory allocation to avoid this.", this);
      
      lowMemoryThreshold = 0;
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
    
    // if a notification and either low on memory or over send queue length...
    if (writeRequest.getMessage () instanceof NotifyDeliver && 
        (inLowMemoryState () || 
          session.getScheduledWriteBytes () > sendQueueMaxLength (session)))
    {
      boolean disconnect = sendQueueDropPolicy (session).equals ("fail");
      
      if (disconnect)
      {
        warnQueueOverflow (session, "closing connection");
        
        session.close (true);
      } else 
      {
        if (session.getAttribute (IN_DROPWARN_STATE) == null)
        {
          warnQueueOverflow (session, "started dropping packets");
          
          session.setAttribute (IN_DROPWARN_STATE);
          
          super.filterWrite (nextFilter, session, 
                             new DefaultWriteRequest (new DropWarn ()));
        }
      }
      
      // drop NotifyDeliver
      return;
    }
    
    if (session.getAttribute (IN_DROPWARN_STATE) != null)
    {
      session.removeAttribute (IN_DROPWARN_STATE);
      
      diagnostic 
        ("Stopped dropping packets for client " + idFor (session), this);
    }
    
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
    if (!inLowMemoryState () && freeMemory () < lowMemoryThreshold)
    {      
      synchronized (this)
      {
        System.gc ();
        
        long freeMemory = freeMemory ();
        
        if (freeMemory < lowMemoryThreshold && !inLowMemoryState ())
        {
          warn ("Entering low memory state: throttling clients: " +
                formatBytes (freeMemory) + " < " + 
                formatBytes (lowMemoryThreshold) + " bytes free", this);        
  
          enterLowMemoryState ();
        }
      }
    }
  }

  private void enterLowMemoryState ()
  {
    poller = new PollingThread ();
    poller.start ();
  }

  private boolean inLowMemoryState ()
  {
    return poller != null;
  }

  protected void throttle ()
  {
    for (IoAcceptor acceptor : ioManager.acceptors ())
      acceptor.unbind ();
    
    for (IoSession session : ioManager.sessions ())
      session.suspendRead ();
  }
  
  protected void unthrottle ()
  {
    synchronized (this)
    {
      poller = null;
      
      for (IoAcceptor acceptor : ioManager.acceptors ())
      {
        try
        {
          acceptor.bind ();
        } catch (IOException ex)
        {
          alarm ("Failed to rebind acceptor for " + 
                 acceptor.getDefaultLocalAddresses () + 
                 " after low memory throttle", this, ex);
        }
      }
      
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
  
  protected static String formatBytes (long bytes)
  {
    return String.format ("%,d", bytes);
  }
  
  protected static long freeMemory ()
  {
    /*
     * Using getRuntime ().freeMemory () on the 1.6 server VM appears
     * to sometimes just return (committed - used), which can be far
     * smaller than what could be available. Also, System.gc () has no
     * effect in this case, so low memory could be fired early and
     * never exit. So, we use (max - used), which is the most
     * optimistic case (a memory alloc could still fail due to lack of
     * system RAM).
     */
    MemoryUsage memory = getMemoryMXBean ().getHeapMemoryUsage ();
    
    return memory.getMax () - memory.getUsed ();
  }

  private static void warnQueueOverflow (IoSession session, String action)
  {
    warn 
      ("Client " + idFor (session) + " has overflowed its maximum send " +
       "queue size (Send-Queue.Max-Length) of " + 
       formatNumber (sendQueueMaxLength (session)) + ": " + action, 
       LowMemoryProtectionFilter.class);
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
      throttle ();
      
      long lastGc = 0;
      long lastKill = 0;
      
      try
      {
        while (freeMemory () < lowMemoryThreshold)
        {
          long now = currentTimeMillis ();
          
          if (now - lastKill > 500)
          {            
            killBiggestClient ();            
            lastKill = now;
          }
          
          if (now - lastGc > 2000)
          {
            System.gc ();
            lastGc = now;
          }
          
          if (shouldLog (DIAGNOSTIC))
          {
            diagnostic 
              ("Low memory manager: " + formatBytes (freeMemory ()) + 
               " free memory", this);
          }
          
          sleep (250);
        }
      } catch (InterruptedException ex)
      {
        interrupt ();
      } finally
      {
        diagnostic ("Leaving low memory state: unthrottling clients: " +
                    formatBytes (freeMemory ()) + " > " + 
                    formatBytes (lowMemoryThreshold) + " bytes free", this);
        
        unthrottle ();
      }
    }

    /**
     * Kil the client using the most bandwidth.
     */
    private void killBiggestClient ()
    {
      long maxThrougput = 0;
      IoSession busiestClient = null;
      
      for (IoSession session : ioManager.sessions ())
      {
        updateThroughput (session);
        
        long throughput = (long)readBytesThroughput (session);
        
        if (throughput > maxThrougput)
        {
          maxThrougput = throughput;          
          busiestClient = session;
        }
      }
      
      if (busiestClient != null)
      {
        diagnostic ("Low memory manager: killing client " + 
                    idFor (busiestClient) + " with " + 
                    formatBytes (maxThrougput) + " throughput", this);
        
        busiestClient.close (true);
      }
    }
  }
}
