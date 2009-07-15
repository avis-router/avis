package org.avis.logging;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;

import static java.lang.Thread.sleep;

import static java.lang.Math.random;

import static org.junit.Assert.*;

public class JUTestLogMessageBuffer
{
  private int serial = 1;

  @Test
  public void expiry ()
    throws Exception
  {
    LogMessageBuffer buffer = new LogMessageBuffer (10);
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
    final LogMessageBuffer buffer = new LogMessageBuffer (10);
    ArrayList<Thread> threads = new ArrayList<Thread> ();
    
    for (int i = 0; i < 10; i++)
    {
      threads.add (new Thread ()
      {
        @Override
        public void run ()
        {
          try
          {
            while (!interrupted ())
            {
              sleep ((long)random () * 100);
              
              buffer.add (logEvent ());
              assertTrue (buffer.eventCount () <= 10);              
            }
          } catch (InterruptedException ex)
          {
            interrupt ();
          }
        }
      });
    }
    
    for (Thread thread : threads)
      thread.start ();
    
    sleep (5000);
    
    for (Thread thread : threads)
      thread.interrupt ();
    
    assertEquals (10, buffer.eventCount ());
    assertEquals (10, buffer.events.size ());
  }

  private static void assertHead (int serial, LogMessageBuffer buffer)
  {
    assertEquals (serial, Integer.valueOf (buffer.head ().message));
  }
  
  private static void assertTail (int serial, LogMessageBuffer buffer)
  {
    assertEquals (serial, Integer.valueOf (buffer.tail ().message));
  }

  protected LogEvent logEvent ()
  {
    return new LogEvent (JUTestLogMessageBuffer.class, new Date (), 
                         Log.INFO, Integer.toString (serial++), null);
  }
}
