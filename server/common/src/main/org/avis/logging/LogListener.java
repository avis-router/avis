package org.avis.logging;

/**
 * A listener to log messages.
 * 
 * @see Log#addLogListener(LogListener)
 * 
 * @author Matthew Phillips
 */
public interface LogListener
{
  /**
   * Invoked when a message is posted to the log.
   */
  public void messageLogged (LogEvent e);
}
