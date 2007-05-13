package org.avis.util;

import java.util.ArrayList;
import java.util.List;

import dsto.dfc.logging.LogEvent;
import dsto.dfc.logging.LogListener;

import static dsto.dfc.logging.Log.ALARM;
import static dsto.dfc.logging.Log.INTERNAL_ERROR;
import static dsto.dfc.logging.Log.WARNING;
import static dsto.dfc.logging.Log.addLogListener;
import static dsto.dfc.logging.Log.removeLogListener;
import static dsto.dfc.logging.Log.toLogString;

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
  
  public void messageReceived (LogEvent e)
  {
    int type = e.getType ();
    
    if (type == ALARM ||
        type == INTERNAL_ERROR ||
        type == WARNING)
    {
      errors.add (e);
    }
  }
}