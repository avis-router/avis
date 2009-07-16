package org.avis.logging;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.min;

/**
 * Stores log messages emitted by the system log. This buffer uses
 * soft, non-blocking concurrency control which allows the buffer to
 * temporarily go above the max event count if under heavy concurrent
 * use.
 * 
 * @version $Revision: 1.17 $
 */
public class LogEventBuffer implements LogListener, Iterable<LogEvent>
{
  protected int maxEvents;
  protected AtomicInteger eventCount;
  protected ConcurrentLinkedQueue<LogEvent> events;

  public LogEventBuffer ()
  {
    this (1000);
  }
  
  public LogEventBuffer (int maxEvents)
  {
    this.maxEvents = maxEvents;
    this.eventCount = new AtomicInteger (0);
    this.events = new ConcurrentLinkedQueue<LogEvent> ();

    Log.addLogListener (this);
  }

  public void dispose ()
  {
    Log.removeLogListener (this);
  }

  public int eventCount ()
  {
    return eventCount.get ();
  }

  public void messageLogged (LogEvent e)
  {
    add (e);
  }
  
  public void clear ()
  {
    eventCount.set (0);
    events.clear ();
  }

  public Iterator<LogEvent> iterator ()
  {
    return events.iterator ();
  }
  
  /**
   * Add an event to the buffer.
   */
  public void add (LogEvent event)
  {
    expireEvents ();

    events.add (event);
  }

  /**
   * Expire any excess events from the buffer so that a new event may
   * be added.
   */
  protected void expireEvents ()
  {
    for (;;)
    {
      // compute the old event count, and what I would like it to be
      int oldCount = eventCount.get ();
      int newCount = min (maxEvents, oldCount + 1);
      
      // try to take that many events from the count
      if (eventCount.compareAndSet (oldCount, newCount))
      {
        /* if we succeed, remove the events from the queue and exit,
           otherwise try again */
        for (int i = oldCount - newCount; i >= 0; i--)
          events.remove ();
        
        break;
      }
    }
  }

  public LogEvent head ()
  {
    return events.element ();
  }
 
  /**
   * NB: this is very inefficient.
   */
  public LogEvent tail ()
  {
    return (LogEvent)events.toArray () [events.size () - 1];
  }
}
