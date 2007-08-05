package org.avis.federation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFilter.NextFilter;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.messages.Message;
import org.avis.io.messages.RequestMessage;
import org.avis.io.messages.XidMessage;

import static java.lang.Math.min;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Tracks outstanding requests and allows them to be correlated with
 * their replies. When a timeout expires for a request, a synthetic
 * TimeoutMessage is generated and posted to the session.
 * 
 * @author Matthew Phillips
 */
public class RequestTracker implements Runnable
{
  private IoSession session;
  private ScheduledExecutorService executor;
  private Map<Integer, Entry> xidToRequest;
  private ScheduledFuture<?> timeoutFuture;
  private int timeout;

  /**
   * Create a new instance.
   */
  public RequestTracker (IoSession session)
  {
    this.session = session;
    this.executor = newScheduledThreadPool (1);
    this.xidToRequest = new HashMap<Integer, Entry> ();
    this.timeout = 60;
  }
  
  public synchronized void shutdown ()
  {
    cancelTimeoutCheck ();
    
    executor.shutdown ();
  }

  /**
   * Set the timeout (in seconds) before a request is deemed to have
   * not been responded to.
   */
  public synchronized void setTimeout (int newTimeout)
  {
    cancelTimeoutCheck ();
    
    this.timeout = newTimeout;
    
    scheduleTimeoutCheck (0);
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
   * Add a request to the tracker.
   */
  public synchronized void add (RequestMessage<?> request)
  {
    xidToRequest.put (request.xid, new Entry (request));
    
    scheduleTimeoutCheck (timeout);
  }
  
  /**
   * Remove a request from the tracker given its reply.
   * 
   * @param reply The reply message. 
   * 
   * @return The request message, or null if there is none for the reply.
   */
  public synchronized RequestMessage<?> remove (XidMessage reply)
  {
    Entry entry = xidToRequest.remove (reply.xid);
    
    if (entry == null)
      return null;
    else
      return entry.request;
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
        
        NextFilter filter = session.getFilterChain ().getNextFilter ("codec");
        
        filter.messageReceived (session, 
                                new TimeoutMessage (entry.request));
      } else
      {
        earliest = min (earliest, entry.sentAt);
      }
    }
    
    if (!xidToRequest.isEmpty ())
      scheduleTimeoutCheck ((int)((now - earliest) / 1000) + timeout);
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
  
  /**
   * Synthetic message sent when a request times out. 
   */
  public static class TimeoutMessage extends Message
  {
    public static final int ID = 100000;
    
    /**
     * The request that timed out.
     */
    public final RequestMessage<?> request;

    TimeoutMessage (RequestMessage<?> request)
    {
      this.request = request;
    }

    @Override
    public void decode (ByteBuffer in) 
      throws ProtocolCodecException
    {
      throw new UnsupportedOperationException ("Synthetic message");
    }

    @Override
    public void encode (ByteBuffer out) 
      throws ProtocolCodecException
    {
      throw new UnsupportedOperationException ("Synthetic message");
    }

    @Override
    public int typeId ()
    {
      return ID;
    }
  }
}
