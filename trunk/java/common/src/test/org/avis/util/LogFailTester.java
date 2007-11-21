package org.avis.util;

import java.util.ArrayList;
import java.util.List;

import org.avis.logging.LogEvent;
import org.avis.logging.LogListener;

import static org.avis.logging.Log.ALARM;
import static org.avis.logging.Log.WARNING;
import static org.avis.logging.Log.addLogListener;
import static org.avis.logging.Log.enableLogging;
import static org.avis.logging.Log.removeLogListener;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.toLogString;
import static org.junit.Assert.fail;

/**
 * Automatically fails a test if an error or warning is logged.
 * 
 * @author Matthew Phillips
 */
public class LogFailTester implements LogListener
{
  private List<LogEvent> errors;
  private volatile boolean paused;
  private boolean wasLoggingErrors;
  
  public LogFailTester ()
  {
    this.errors = new ArrayList<LogEvent> ();
    
    addLogListener (this);
  }
  
  /**
   * Pause logging and detecting errors/warnings.
   */
  public void pause ()
  {
    if (paused)
      return;
    
    paused = true;
    
    wasLoggingErrors = shouldLog (ALARM);
    
    enableLogging (ALARM, false);
    enableLogging (WARNING, false);
  }
  
  /**
   * Reverse the effect of pause.
   */
  public void unpause ()
  {
    if (!paused)
      return;
    
    paused = false;
    
    enableLogging (ALARM, wasLoggingErrors);
    enableLogging (WARNING, wasLoggingErrors);
  }
  
  /**
   * Assert there were no errors/warnings logged and dispose.
   */
  public void assertOkAndDispose ()
  {
    removeLogListener (this);

    if (!errors.isEmpty ())
    {
      StringBuilder errorMessages = new StringBuilder ();

      for (LogEvent e : errors)
        errorMessages.append (toLogString (e)).append ('\n');
      
      fail ("Errors/warnings in log: " + errorMessages);
    }
  }
  
  public void messageLogged (LogEvent e)
  {
    if (!paused && e.type >= WARNING)
      errors.add (e);
  }
}