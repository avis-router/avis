package org.avis.util;

import java.util.ArrayList;

import java.io.Closeable;

import static org.avis.logging.Log.alarm;

/**
 * Test utility to manage automatic close () of resources. Call close () on
 * test cleanup.
 * 
 * @author Matthew Phillips
 */
public class AutoClose
{
  private ArrayList<Closeable> autoClose;

  public AutoClose ()
  {
    autoClose = new ArrayList<Closeable> ();
  }

  public void add (Closeable toClose)
  {
    autoClose.add (toClose);
  }
  
  public void close ()
  {
    for (int i = autoClose.size () - 1; i >= 0; i--)
    {
      try
      {
        autoClose.get (i).close ();
      } catch (Throwable ex)
      {
        alarm ("Failed to close", this, ex);
      }
    }
    
    autoClose.clear ();
  }
}
