package org.avis.router;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import static java.lang.System.currentTimeMillis;

/**
 * Implements updating of session-level stats. MINA has a stats API, but as
 * of 2.0M6 doesn't actually update it.
 * 
 * @author Matthew Phillips
 */
public class StatsFilter extends IoFilterAdapter
{
  public static final IoFilter INSTANCE = new StatsFilter ();

  private StatsFilter ()
  {
    // zip
  }
  
  @Override
  public void sessionCreated (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    session.setAttribute ("statsLock", new Object ());

    super.sessionCreated (nextFilter, session);
  }
  
  @Override
  public void messageReceived (NextFilter nextFilter,
                               IoSession session, Object message)
    throws Exception
  {
    super.messageReceived (nextFilter, session, message);
    
    if (message instanceof IoBuffer)
    {
      synchronized (lockFor (session))
      {
        long now = currentTimeMillis ();
        AbstractIoSession ioSession = (AbstractIoSession)session;
        
        ioSession.increaseReadBytes (((IoBuffer)message).limit (), now);
        ioSession.updateThroughput (now, false);
      }
    }
  }
  
  private static Object lockFor (IoSession session)
  {
    Object lock = session.getAttribute ("statsLock");
    
    if (lock == null)
      throw new IllegalStateException ("No stats filter for session");
    
    return lock;
  }

  @Override
  public void messageSent (NextFilter nextFilter, IoSession session,
                           WriteRequest writeRequest)
    throws Exception
  {
    super.messageSent (nextFilter, session, writeRequest);
    
    if (writeRequest.getMessage () instanceof IoBuffer)
    {
      synchronized (lockFor (session))
      {
        long now = currentTimeMillis ();
        AbstractIoSession ioSession = (AbstractIoSession)session;
        
        ioSession.increaseReadBytes 
          (((IoBuffer)writeRequest.getMessage ()).limit (), now);
        ioSession.updateThroughput (now, false);
      }
    }
  }

  /**
   * Trigger a throughput update. Like AbstractIoSession.updateThroughput () 
   * but thread-safe.
   */
  public static void updateThroughput (IoSession session)
  {
    Object lock = lockFor (session);
    
    // TODO work out why this is null in rare cases
    if (lock != null)
    {
      synchronized (lock)
      {
        ((AbstractIoSession)session).updateThroughput (currentTimeMillis (), 
                                                       false);
      }
    }
  }

  /**
   * Get bytes/sec read throughput. Like 
   * AbstractIoSession.getReadBytesThroughput () but thread-safe.
   */
  public static double readBytesThroughput (IoSession session)
  {
    synchronized (lockFor (session))
    {
      return session.getReadBytesThroughput ();
    }
  }
  
  /**
   * Get bytes/sec write throughput. Like 
   * AbstractIoSession.writtenBytesThroughput () but thread-safe.
   */
  public static double writtenBytesThroughput (IoSession session)
  {
    synchronized (lockFor (session))
    {
      return session.getWrittenBytesThroughput ();
    }
  }
}
