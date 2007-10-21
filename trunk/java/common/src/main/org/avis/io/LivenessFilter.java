package org.avis.io;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;

import org.avis.io.messages.ConfConn;
import org.avis.io.messages.LivenessFailureMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.TestConn;

import static java.lang.Math.max;
import static java.lang.Math.random;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.avis.logging.Log.trace;

/**
 * A MINA I/O filter that adds liveness checking using
 * TestConn/ConfConn. Generates LivenessTimeoutMessage's when
 * livenessTimeout passes and no response to a TestConn is seen within
 * replyTimeout seconds.
 * 
 * @author Matthew Phillips
 */
public class LivenessFilter extends IoFilterAdapter implements IoFilter
{
  protected int receiveTimeout;
  protected int livenessTimeout;
  protected String filterName;
  protected ScheduledExecutorService executor;
  
  /**
   * Create a new instance. Uses a {@link SharedExecutor}.
   * 
   * @param livenessTimeout The time (in millis) that must pass before
   *                a liveness check is issued.
   * @param receiveTimeout The amount of time (in millis) to wait for
   *                a reply.
   */
  public LivenessFilter (int livenessTimeout, int receiveTimeout)
  {
    this (null, livenessTimeout, receiveTimeout);
  }
  
  /**
   * Create a new instance.
   * 
   * @param executor The executor to use for timed callbacks.
   * @param livenessTimeout The time (in millis) that must pass before
   *                a liveness check is issued.
   * @param receiveTimeout The amount of time (in millis) to wait for
   *                a reply.
   */
  public LivenessFilter (ScheduledExecutorService executor,
                         int livenessTimeout, int receiveTimeout)
  {
    this.executor = executor;
    this.livenessTimeout = livenessTimeout;
    this.receiveTimeout = receiveTimeout;
  }
  
  /**
   * Force dispose the tracker for a given session.
   */
  public static void dispose (IoSession session)
  {
    trackerFor (session).dispose ();
  }

  public static LivenessFilter filterFor (IoSession session)
  {
    return trackerFor (session).filter ();
  }
  
  public int livenessTimeout ()
  {
    return livenessTimeout;
  }
  
  public static void setLivenessTimeoutFor (IoSession session, int newTimeout)
  {
    if (newTimeout < 1000)
      throw new IllegalArgumentException 
        ("Timeout cannot be < 1000: " + newTimeout);
    
    Tracker tracker = trackerFor (session);
    
    tracker.filter ().livenessTimeout = newTimeout;
    tracker.timeoutUpdated ();
  }
  
  public static void setReceiveTimeoutFor (IoSession session, int newTimeout)
  {
    if (newTimeout < 0)
      throw new IllegalArgumentException 
        ("Timeout cannot be < 0: " + newTimeout);
    
    Tracker tracker = trackerFor (session);
    
    tracker.filter ().receiveTimeout = newTimeout;
    tracker.timeoutUpdated ();
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
  public void sessionCreated (NextFilter nextFilter, 
                              IoSession session)
  throws Exception
  {
    if (executor == null)
      executor = SharedExecutor.acquire ();
    
    nextFilter.sessionCreated (session);
  }
  
  @Override
  public void filterClose (NextFilter nextFilter, 
                           IoSession session)
    throws Exception
  {
    SharedExecutor.release (executor);

    nextFilter.filterClose (session);
  }
  
  @Override
  public void sessionOpened (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    session.setAttribute ("livenessTracker", new Tracker (session));
    
    nextFilter.sessionOpened (session);
  }
  
  static Tracker trackerFor (IoSession session)
  {
    return (Tracker)session.getAttribute ("livenessTracker");
  }
  
  @Override
  public void sessionClosed (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    trackerFor (session).dispose ();
    
    nextFilter.sessionClosed (session);
  }
  
  @Override
  public void messageReceived (NextFilter nextFilter,
                               IoSession session, Object message)
    throws Exception
  {
    trackerFor (session).connectionIsLive ();
    
    // do not forward ConfConn liveness replies
    if (message == ConfConn.INSTANCE)
    {
      trace ("Liveness confirmed: received ConfConn", this);
      
      return;
    }
    
    nextFilter.messageReceived (session, message);
  }
  
  /**
   * An instance of this is attached to each session to track liveness.
   */
  class Tracker
  {
    private IoSession session;
    private ScheduledFuture<?> livenessFuture;
    private long lastLive;
    private long lastTestConnCheck;
    
    public Tracker (IoSession session)
    {
      this.session = session;
      this.lastLive = currentTimeMillis ();
      
      /*
       * Run a liveness check with a randomised delay offset. Helps to
       * avoid two hosts syncing their checks and doubling up on
       * messages.
       */
      scheduleLivenessCheck 
        (livenessTimeout - (long)(random () * livenessTimeout));
    }
    
    public LivenessFilter filter ()
    {
      return LivenessFilter.this;
    }

    public synchronized void dispose ()
    {
      cancelLivenessCheck ();
      
      session = null;
    }
    
    public boolean isDisposed ()
    {
      return livenessFuture == null && session == null;
    }
    
    /**
     * Call to reset liveness timeout.
     */
    public synchronized void connectionIsLive ()
    {
      lastLive = currentTimeMillis ();
    }
    
    public void timeoutUpdated ()
    {
      cancelLivenessCheck ();
      
      lastLive = currentTimeMillis ();
      
      scheduleLivenessCheck ();
    }
    
    private void scheduleLivenessCheck ()
    {
      scheduleLivenessCheck 
        (max (0, livenessTimeout - (currentTimeMillis () - lastLive)));
    }
    
    private void scheduleLivenessCheck (long delay)
    {
      if (livenessFuture == null)
      {
        livenessFuture = executor.schedule 
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

    /**
     * Check if liveness timeout has expired: if so send TestConn and
     * schedule checkConnReply ().
     */
    protected synchronized void checkLiveness ()
    {
      livenessFuture = null;
      
      if (currentTimeMillis () - lastLive >= livenessTimeout)
      {
        trace ("Liveness timeout: sending TestConn", this);
        
        lastTestConnCheck = currentTimeMillis ();
        
        session.write (TestConn.INSTANCE);
        
        livenessFuture = executor.schedule 
          (new Runnable ()
          {
            public void run ()
            {
              checkConnReply ();
            }
          }, receiveTimeout, MILLISECONDS);
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
      
      if (lastLive < lastTestConnCheck)
      {
        trace ("No reply to TestConn: signaling liveness failure", this);
        
        injectMessage (new LivenessFailureMessage ());
      } else
      {
        scheduleLivenessCheck ();
      }
    }

    private void injectMessage (Message message)
    {
      NextFilter filter = 
        session.getFilterChain ().getNextFilter (filterName);
      
      filter.messageReceived (session, message);
    }
  }
}
