package org.avis.logging;

import java.util.Date;

/**
 * An event sent by the log to
 * {@linkplain Log#addLogListener(LogListener) registered listeners}
 * when a message is logged.
 * 
 * @see Log#addLogListener(LogListener)
 * 
 * @author Matthew Phillips
 */
public class LogEvent
{
  public final Date time;
  public final Object source;
  public final int type;
  public final String message;
  public final Throwable exception;
  
  public LogEvent (Object source, Date time, int type,
                   String message, Throwable exception)
  {
    this.source = source;
    this.time = time;
    this.type = type;
    this.message = message;
    this.exception = exception;
  }
}
