package org.avis.io;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;

import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.RequestMessage;
import org.avis.io.messages.TimeoutMessage;
import org.avis.io.messages.XidMessage;

import static java.lang.Math.min;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A MINA I/O filter that adds tracking for XID-based
 * RequestMessage's.
 * 
 * <ul>
 * <li>Automatically fills in the {@link XidMessage#request} field of
 * XidMessage's</li>
 * <li>Generates ErrorMessage's in place of XID-based replies that
 * have no associated request</li>
 * <li>Generates TimeoutMessage's for requests that do not receive a
 * reply within a given timeout.</li>
 * <ul>
 * 
 * @author Matthew Phillips
 */
public class RequestTrackingFilter 
  extends IoFilterAdapter implements IoFilter
{
  private static int instanceCount;
  
  protected static ScheduledExecutorService executor;
  
  protected int timeout;
  protected String filterName;
  
  public RequestTrackingFilter (int timeout)
  {
    this.timeout = timeout;
  }
  
  public boolean sharedResourcesDisposed ()
  {
    return instanceCount == 0;
  }
  
  @Override
  public synchronized void onPreAdd (IoFilterChain parent, 
                                     String name,
                                     NextFilter nextFilter) 
    throws Exception
  {
    this.filterName = name;
  }
  
  @Override
  public synchronized void filterClose (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    if (--instanceCount == 0)
      destroy ();
    
    nextFilter.filterClose (session);
  }
  
  @Override
  public void init () 
    throws Exception
  {
    executor = newScheduledThreadPool (1);
  }
  
  @Override
  public void destroy () 
    throws Exception
  {
    executor.shutdown ();
    executor = null;
  }
  
  @Override
  public synchronized void sessionCreated (NextFilter nextFilter, 
                                           IoSession session)
    throws Exception
  {
    if (instanceCount++ == 0)
      init ();
    
    session.setAttribute ("requestTracker", new Tracker (session));
  }
  
  @Override
  public synchronized void sessionClosed (NextFilter nextFilter, 
                                          IoSession session)
    throws Exception
  {
    trackerFor (session).dispose ();
  }
  
  @Override
  public void filterWrite (NextFilter nextFilter, 
                           IoSession session,
                           WriteRequest writeRequest)
    throws Exception
  {
    Object message = writeRequest.getMessage ();
    
    if (message instanceof RequestMessage<?>)
      trackerFor (session).add ((RequestMessage<?>)message);
    
    nextFilter.filterWrite (session, writeRequest);
  }
  
  @Override
  public void messageReceived (NextFilter nextFilter,
                               IoSession session, Object message)
    throws Exception
  {
    if (message instanceof XidMessage && 
        !(message instanceof RequestMessage<?>))
    {
      XidMessage reply = (XidMessage)message;
      
      try
      {
        reply.request = trackerFor (session).remove (reply);
      } catch (IllegalArgumentException ex)
      {
        message = new ErrorMessage (ex, reply);
      }
    }
    
    nextFilter.messageReceived (session, message);
  }
  
  private static Tracker trackerFor (IoSession session)
  {
    return (Tracker)session.getAttribute ("requestTracker");
  }
  
  /**
   * An instance of this is attached to each session.
   */
  class Tracker implements Runnable
  {
    private IoSession session;
    private Map<Integer, Entry> xidToRequest;
    private ScheduledFuture<?> timeoutFuture;
    
    public Tracker (IoSession session)
    {
      this.session = session;
      this.xidToRequest = new HashMap<Integer, Entry> ();
    }
    
    public synchronized void dispose ()
    {
      cancelTimeoutCheck ();
    }

    public synchronized void add (RequestMessage<?> request)
    {
      xidToRequest.put (request.xid, new Entry (request));
      
      scheduleTimeoutCheck (timeout);
    }
    
    public synchronized RequestMessage<?> remove (XidMessage reply)
    {
      Entry entry = xidToRequest.remove (reply.xid);
      
      if (entry == null)
        throw new IllegalArgumentException 
          ("Reply with unknown XID " + reply.xid);
      else
        return entry.request;
    }
    
    private void cancelTimeoutCheck ()
    {
      if (timeoutFuture != null)
      {
        timeoutFuture.cancel (false);
        
        timeoutFuture = null;
      }
    }
    
    private void scheduleTimeoutCheck (int delay)
    {
      if (timeoutFuture == null)
        timeoutFuture = executor.schedule (this, delay, SECONDS);
    }
    
    /**
     * Called periodically to check for timed-out requests.
     */
    public synchronized void run ()
    {
      timeoutFuture = null;
      
      long now = currentTimeMillis ();
      long earliest = now;
      
      for (Iterator<Map.Entry<Integer, Entry>> i = 
             xidToRequest.entrySet ().iterator (); i.hasNext (); )
      {
        Entry entry = i.next ().getValue ();
        
        if ((now - entry.sentAt) / 1000 >= timeout)
        {
          i.remove ();
          
          NextFilter filter = 
            session.getFilterChain ().getNextFilter (filterName);
          
          filter.messageReceived 
            (session, new TimeoutMessage (entry.request));
        } else
        {
          earliest = min (earliest, entry.sentAt);
        }
      }
      
      if (!xidToRequest.isEmpty ())
        scheduleTimeoutCheck ((int)((now - earliest) / 1000) + timeout);
    }
  }
  
  static class Entry
  {
    public long sentAt;
    public RequestMessage<?> request;

    public Entry (RequestMessage<?> request)
    {
      this.request = request;
      this.sentAt = currentTimeMillis ();
    }
  }
}
