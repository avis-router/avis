package org.avis.io;

import org.apache.mina.common.ExceptionMonitor;

import static org.avis.logging.Log.internalError;

/**
 * MINA exception monitor that routes exceptions to the log.
 * 
 * @author Matthew Phillips
 */
public class ExceptionMonitorLogger extends ExceptionMonitor
{
  public static final ExceptionMonitorLogger INSTANCE = 
    new ExceptionMonitorLogger ();

  private ExceptionMonitorLogger ()
  {
    // zip
  }
  
  @Override
  public void exceptionCaught (Throwable cause)
  {
    internalError ("Unexpected exception during IO", this, cause);
  }
}
