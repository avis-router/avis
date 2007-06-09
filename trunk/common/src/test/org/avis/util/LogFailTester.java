package org.avis.util;

import java.util.ArrayList;
import java.util.List;

import org.avis.logging.LogEvent;
import org.avis.logging.LogListener;

import static org.avis.logging.Log.WARNING;
import static org.avis.logging.Log.addLogListener;
import static org.avis.logging.Log.removeLogListener;
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
  
  public LogFailTester ()
  {
    this.errors = new ArrayList<LogEvent> ();
    
    addLogListener (this);
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
    if (e.type >= WARNING)
      errors.add (e);
  }
}