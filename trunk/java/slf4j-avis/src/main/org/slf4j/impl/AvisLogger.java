package org.slf4j.impl;

import org.slf4j.helpers.MarkerIgnoringBase;

import org.avis.logging.Log;

import static org.slf4j.helpers.MessageFormatter.arrayFormat;
import static org.slf4j.helpers.MessageFormatter.format;

/**
 * Redirect slf4j log messages to the Avis logger.
 */
public class AvisLogger extends MarkerIgnoringBase
{
  public static int [] LEVEL_MAP = new int [Log.MAX_LEVEL + 1];
  
  static
  {
    // map everything to trace: MINA is very chatty
    for (int i = 0; i < LEVEL_MAP.length; i++)
      LEVEL_MAP [i] = Log.TRACE;
    
    // allow errors and warnings through by default
    LEVEL_MAP [Log.ALARM] = Log.ALARM;
    LEVEL_MAP [Log.WARNING] = Log.WARNING;
  }
  
  AvisLogger (String name)
  {
    this.name = name;
  }
  
  public boolean isTraceEnabled ()
  {
    return Log.shouldLog (LEVEL_MAP [Log.TRACE]);
  }

  public void trace (String msg)
  {
    log (Log.TRACE, msg);
  }

  public void trace (String format, Object param1)
  {
    formatAndLog (Log.TRACE, format, param1);
  }

  public void trace (String format, Object param1, Object param2)
  {
    formatAndLog (Log.TRACE, format, param1, param2);
  }

  public void trace (String format, Object [] argArray)
  {
    formatAndLog (Log.TRACE, format, argArray);
  }

  public void trace (String msg, Throwable t)
  {
    log (Log.TRACE, msg, t);
  }

  public boolean isDebugEnabled ()
  {
    return Log.shouldLog (LEVEL_MAP [Log.DIAGNOSTIC]);
  }

  public void debug (String msg)
  {
    log (Log.DIAGNOSTIC, msg);
  }

  public void debug (String format, Object param1)
  {
    formatAndLog (Log.TRACE, format, param1);
  }

  public void debug (String format, Object param1, Object param2)
  {
    formatAndLog (Log.TRACE, format, param1, param2);
  }

  public void debug (String format, Object [] argArray)
  {
    formatAndLog (Log.TRACE, format, argArray);
  }

  public void debug (String msg, Throwable t)
  {
    log (Log.DIAGNOSTIC, msg, t);
  }
  
  public boolean isInfoEnabled ()
  {
    return Log.shouldLog (LEVEL_MAP [Log.INFO]);
  }

  public void info (String msg)
  {
    log (Log.INFO, msg);
  }

  public void info (String format, Object arg)
  {
    formatAndLog (Log.INFO, format, arg);
  }

  public void info (String format, Object arg1, Object arg2)
  {
    formatAndLog (Log.INFO, format, arg1, arg2);
  }

  public void info (String format, Object [] argArray)
  {
    formatAndLog (Log.INFO, format, argArray);
  }

  public void info (String msg, Throwable t)
  {
    log (Log.INFO, msg, t);
  }

  public boolean isWarnEnabled ()
  {
    return Log.shouldLog (LEVEL_MAP [Log.WARNING]);
  }

  public void warn (String msg)
  {
    log (Log.WARNING, msg);
  }

  public void warn (String format, Object arg)
  {
    formatAndLog (Log.WARNING, format, arg, this);
  }

  public void warn (String format, Object arg1, Object arg2)
  {
    formatAndLog (Log.WARNING, format, arg1, arg2);
  }

  public void warn (String format, Object [] argArray)
  {
    formatAndLog (Log.WARNING, format, argArray);
  }

  public void warn (String msg, Throwable t)
  {
    log (Log.WARNING, msg, t);
  }

  public boolean isErrorEnabled ()
  {
    return Log.shouldLog (LEVEL_MAP [Log.ALARM]);
  }

  public void error (String msg)
  {
    log (Log.ALARM, msg);
  }

  public void error (String format, Object arg)
  {
    formatAndLog (Log.ALARM, format, arg);
  }

  public void error (String format, Object arg1, Object arg2)
  {
    formatAndLog (Log.ALARM, format, arg1, arg2);
  }

  public void error (String format, Object [] argArray)
  {
    formatAndLog (Log.ALARM, format, argArray);
  }

  public void error (String msg, Throwable t)
  {
    log (Log.ALARM, msg, t);
  }

  private static void formatAndLog (int level, String format, Object arg1)
  {
    int actualLevel = LEVEL_MAP [level];
    
    if (Log.shouldLog (actualLevel))
      log (level, format (format, arg1));
  }

  private static void formatAndLog (int level, String format,
                                    Object arg1, Object arg2)
  {
    int actualLevel = LEVEL_MAP [level];
    
    if (Log.shouldLog (actualLevel))
      log (level, format (format, arg1, arg2));
  }

  private static void formatAndLog (int level, String format,
                                    Object [] argArray)
  {
    int actualLevel = LEVEL_MAP [level];
    
    if (Log.shouldLog (actualLevel))
      log (level, arrayFormat (format, argArray));
  }

  private static void log (int level, String msg)
  {
    log (level, msg, null);
  }  

  private static void log (int level, String msg, Throwable t)
  {
    int actualLevel = LEVEL_MAP [level];
    
    if (Log.shouldLog (actualLevel))
      Log.log (actualLevel, "IO: " + msg, AvisLogger.class, t);
  }  
}
