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

import org.avis.io.messages.ConfConn;
import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.LivenessTimeoutMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.RequestMessage;
import org.avis.io.messages.RequestTimeoutMessage;
import org.avis.io.messages.TestConn;
import org.avis.io.messages.XidMessage;

import static java.lang.Math.min;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.avis.logging.Log.trace;

/**
 * A MINA I/O filter that adds tracking for XID-based RequestMessage's
 * and does session liveness checking using TestConn/ConfConn.
 * 
 * <ul>
 * <li>Automatically fills in the {@link XidMessage#request} field of
 * reply XidMessage's</li>
 * <li>Generates ErrorMessage's in place of XID-based replies that
 * have no associated request</li>
 * <li>Generates TimeoutMessage's for requests that do not receive a
 * reply within a given timeout.</li>
 * <li>Generates LivenessTimeoutMessage's when livenessTimeout passes
 * and no response to a TestConn is seen within replyTimeout seconds.</li>
 * <ul>
 * 
 * @author Matthew Phillips
 */
public class RequestTrackingFilter 
  extends IoFilterAdapter implements IoFilter
{
  protected static int shareCount;
  protected static ScheduledExecutorService sharedExecutor;
  
  protected int replyTimeout;
  protected int livenessTimeout;
  protected String filterName;
  
  public RequestTrackingFilter (int replyTimeout, int livenessTimeout)
  {
    this.replyTimeout = replyTimeout;
    this.livenessTimeout = livenessTimeout;
  }
  
  public boolean sharedResourcesDisposed ()
  {
    synchronized (RequestTrackingFilter.class)
    {
      return shareCount == 0;
    }
  }
  
  @Override
  public void onPreAdd (IoFilterChain parent, String name,
                        NextFilter nextFilter) 
    throws Exception
  {
    this.filterName = name;
  }
  
  @Override
  public void filterClose (NextFilter nextFilter, 
                           IoSession session)
    throws Exception
  {
    synchronized (RequestTrackingFilter.class)
    {
      if (--shareCount == 0)
      {
        sharedExecutor.shutdown ();
        sharedExecutor = null;
      }
    }

    nextFilter.filterClose (session);
  }
  
  @Override
  public void sessionCreated (NextFilter nextFilter, 
                              IoSession session)
    throws Exception
  {
    synchronized (RequestTrackingFilter.class)
    {
      if (shareCount++ == 0)
        sharedExecutor = newScheduledThreadPool (1);
    }
    
    nextFilter.sessionCreated (session);
  }
  
  @Override
  public void sessionOpened (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    session.setAttribute ("requestTracker", new Tracker (session));
    
    nextFilter.sessionOpened (session);
  }
  
  @Override
  public void sessionClosed (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    trackerFor (session).dispose ();
    
    nextFilter.sessionClosed (session);
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
    Tracker tracker = trackerFor (session);
    
    tracker.connectionIsLive ();
    
    // do not forward ConfConn liveness replies
    if (message == ConfConn.INSTANCE)
      return;
    
    if (message instanceof XidMessage && 
        !(message instanceof RequestMessage<?>))
    {
      XidMessage reply = (XidMessage)message;
      
      try
      {
        reply.request = tracker.remove (reply);
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
   * An instance of this is attached to each session to track requests
   * and liveness.
   */
  class Tracker implements Runnable
  {
    private IoSession session;
    private Map<Integer, Entry> xidToRequest;
    private ScheduledFuture<?> replyFuture;
    private ScheduledFuture<?> livenessFuture;
    private long lastLive;
    
    public Tracker (IoSession session)
    {
      this.session = session;
      this.xidToRequest = new HashMap<Integer, Entry> ();
      this.lastLive = currentTimeMillis ();
      
      scheduleLivenessCheck ();
    }
    
    public synchronized void dispose ()
    {
      cancelReplyCheck ();
      cancelLivenessCheck ();
    }
    
    /**
     * Call to reset liveness timeout.
     */
    public synchronized void connectionIsLive ()
    {
      lastLive = currentTimeMillis ();

      scheduleLivenessCheck ();
    }

    private void scheduleLivenessCheck ()
    {
      if (livenessFuture == null)
      {
        livenessFuture = sharedExecutor.schedule 
          (new Runnable ()
          {
            public void run ()
            {
              checkLiveness ();
            }
          }, 
          livenessTimeout - (currentTimeMillis () - lastLive) / 1000, SECONDS);
      }
    }
    
    /**
     * Check if liveness timeout has expired: if so send TestConn and
     * schedule checkConnReply ().
     */
    protected synchronized void checkLiveness ()
    {
      livenessFuture = null;
      
      if ((currentTimeMillis () - lastLive) / 1000 >= livenessTimeout)
      {
        trace ("Liveness timeout: sending TestConn", this);
        
        session.write (TestConn.INSTANCE);
        
        livenessFuture = sharedExecutor.schedule 
          (new Runnable ()
          {
            public void run ()
            {
              checkConnReply ();
            }
          }, replyTimeout, SECONDS);
      } else
      {
        scheduleLivenessCheck ();
      }
    }

    /**
     * If no response seen to TestConn within replyTimeout, send
     * LivenessTimeoutMessage.
     */
    protected synchronized void checkConnReply ()
    {
      livenessFuture = null;
      
      if ((currentTimeMillis () - lastLive) / 1000 >= replyTimeout)
      {
        trace ("No reply to TestConn: sending liveness timeout message", this);
        
        injectMessage (new LivenessTimeoutMessage ());
      } else
      {
        scheduleLivenessCheck ();
      }
    }

    private void cancelLivenessCheck ()
    {
      if (livenessFuture != null)
      {
        livenessFuture.cancel (false);
        
        livenessFuture = null;
      }
    }

    public synchronized void add (RequestMessage<?> request)
    {
      xidToRequest.put (request.xid, new Entry (request));
      
      scheduleReplyCheck (replyTimeout);
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
    
    private void cancelReplyCheck ()
    {
      if (replyFuture != null)
      {
        replyFuture.cancel (false);
        
        replyFuture = null;
      }
    }
    
    private void scheduleReplyCheck (int delay)
    {
      if (replyFuture == null)
        replyFuture = sharedExecutor.schedule (this, delay, SECONDS);
    }
    
    /**
     * Called periodically to check for timed-out requests.
     */
    public synchronized void run ()
    {
      replyFuture = null;
      
      long now = currentTimeMillis ();
      long earliestTimeout = now;
      
      for (Iterator<Map.Entry<Integer, Entry>> i = 
             xidToRequest.entrySet ().iterator (); i.hasNext (); )
      {
        Entry entry = i.next ().getValue ();
        
        if ((now - entry.sentAt) / 1000 >= replyTimeout)
        {
          i.remove ();
          
          injectMessage (new RequestTimeoutMessage (entry.request));
        } else
        {
          earliestTimeout = min (earliestTimeout, entry.sentAt);
        }
      }
      
      if (!xidToRequest.isEmpty ())
      {
        scheduleReplyCheck 
          (replyTimeout - (int)((now - earliestTimeout) / 1000));
      }
    }

    private void injectMessage (Message message)
    {
      NextFilter filter = 
        session.getFilterChain ().getNextFilter (filterName);
      
      filter.messageReceived (session, message);
    }
  }
  
  static class Entry
  {
    public RequestMessage<?> request;
    public long sentAt;

    public Entry (RequestMessage<?> request)
    {
      this.request = request;
      this.sentAt = currentTimeMillis ();
    }
  }
}