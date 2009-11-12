package org.avis.logging;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;

import static java.lang.Thread.sleep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JUTestLogMessageBuffer
{
  private int serial = 1;

  @Test
  public void expiry ()
    throws Exception
  {
    LogEventBuffer buffer = new LogEventBuffer (10);
    assertEquals (0, buffer.eventCount ());
    
    buffer.add (logEvent ());
    assertEquals (1, buffer.eventCount ());
    
    buffer.add (logEvent ());
    assertEquals (2, buffer.eventCount ());
    
    // fill to one less than full
    for (int i = 0; i < 7; i++)
      buffer.add (logEvent ());
    
    assertEquals (9, buffer.eventCount ());
    assertHead (1, buffer);
    
    // fill buffer
    buffer.add (logEvent ());
    assertEquals (10, buffer.eventCount ());
    assertHead (1, buffer);
    assertTail (10, buffer);
    
    // overflow by 1
    buffer.add (logEvent ());
    assertEquals (10, buffer.eventCount ());
    assertHead (2, buffer);
    assertTail (11, buffer);
    
    // overflow by another 4
    for (int i = 0; i < 4; i++)
      buffer.add (logEvent ());
    assertEquals (10, buffer.eventCount ());
    
    assertHead (6, buffer);
    assertTail (15, buffer);
  }
  
  @Test
  public void threads ()
    throws Exception
  {
    final LogEventBuffer buffer = new LogEventBuffer (10);
    ArrayList<Thread> threads = new ArrayList<Thread> ();
    
    for (int i = 0; i < 10; i++)
    {
      threads.add (new Thread ()
      {
        @Override
        public void run ()
        {
          while (!interrupted ())
          {
            buffer.add (logEvent ());
            assertTrue (buffer.eventCount () <= 10);              
          }
        }
      });
    }
    
    for (Thread thread : threads)
      thread.start ();
    
    sleep (5000);
    
    for (Thread thread : threads)
    {
      thread.interrupt ();
      thread.join ();
    }
    
    assertEquals (10, buffer.eventCount ());
    assertEquals (10, buffer.events.size ());
  }

  private static void assertHead (int serial, LogEventBuffer buffer)
  {
    assertEquals (serial, Integer.valueOf (buffer.head ().message));
  }
  
  private static void assertTail (int serial, LogEventBuffer buffer)
  {
    assertEquals (serial, Integer.valueOf (buffer.tail ().message));
  }

  protected LogEvent logEvent ()
  {
    return new LogEvent (JUTestLogMessageBuffer.class, new Date (), 
                         Log.INFO, Integer.toString (serial++), null);
  }
}
