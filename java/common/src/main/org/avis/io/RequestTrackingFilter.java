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
import org.avis.io.messages.LivenessFailureMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.RequestMessage;
import org.avis.io.messages.RequestTimeoutMessage;
import org.avis.io.messages.TestConn;
import org.avis.io.messages.XidMessage;

import static java.lang.Math.min;
import static java.lang.Math.random;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
  
  /**
   * Testing method: simulate a hang by stopping request tracking.
   */
  public void hang (IoSession session)
  {
    trackerFor (session).dispose ();
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
    {
      trace ("Liveness confirmed: received ConfConn", this);
      
      return;
    }
    
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
    private Map<Integer, Request> xidToRequest;
    private ScheduledFuture<?> replyFuture;
    private ScheduledFuture<?> livenessFuture;
    private long lastLive;
    
    public Tracker (IoSession session)
    {
      this.session = session;
      this.xidToRequest = new HashMap<Integer, Request> ();
      this.lastLive = currentTimeMillis ();
      
      /*
       * Run a liveness check with a randomised delay offset. Helps to
       * avoid two hosts syncing their checks and doubling up on
       * messages.
       */
      long delay = livenessTimeout * 1000L;
      
      scheduleLivenessCheck (delay - (long)(random () * delay));
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
      
      if ((currentTimeMillis () - lastLive) / 1000 > replyTimeout)
      {
        trace ("No reply to TestConn: signaling liveness failure", this);
        
        injectMessage (new LivenessFailureMessage ());
      } else
      {
        scheduleLivenessCheck ();
      }
    }

    private void scheduleLivenessCheck ()
    {
      scheduleLivenessCheck 
        ((livenessTimeout * 1000L) - (currentTimeMillis () - lastLive));
    }
    
    private void scheduleLivenessCheck (long delay)
    {
      if (livenessFuture == null)
      {
        System.out.println ("*** check in " + delay);
        livenessFuture = sharedExecutor.schedule 
          (new Runnable ()
          {
            public void run ()
            {
              checkLiveness ();
            }
          }, delay, MILLISECONDS);
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
      xidToRequest.put (request.xid, new Request (request));
      
      scheduleReplyCheck (replyTimeout);
    }
    
    public synchronized RequestMessage<?> remove (XidMessage reply)
    {
      Request request = xidToRequest.remove (reply.xid);
      
      if (request == null)
        throw new IllegalArgumentException 
          ("Reply with unknown XID " + reply.xid);
      else
        return request.message;
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
      
      for (Iterator<Map.Entry<Integer, Request>> i = 
             xidToRequest.entrySet ().iterator (); i.hasNext (); )
      {
        Request request = i.next ().getValue ();
        
        if ((now - request.sentAt) / 1000 >= replyTimeout)
        {
          i.remove ();
          
          injectMessage (new RequestTimeoutMessage (request.message));
        } else
        {
          earliestTimeout = min (earliestTimeout, request.sentAt);
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
  
  static class Request
  {
    public RequestMessage<?> message;
    public long sentAt;

    public Request (RequestMessage<?> request)
    {
      this.message = request;
      this.sentAt = currentTimeMillis ();
    }
  }
}
