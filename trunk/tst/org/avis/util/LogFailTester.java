package org.avis.util;

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
  public LogFailTester ()
  {
    addLogListener (this);
  }
  
  public void dispose ()
  {
    removeLogListener (this);
  }
  
  public void messageReceived (LogEvent e)
  {
    int type = e.getType ();
    
    if (type == ALARM ||
        type == INTERNAL_ERROR ||
        type == WARNING)
    {
      fail ("Logged error: " + toLogString (e));
    }
  }
}