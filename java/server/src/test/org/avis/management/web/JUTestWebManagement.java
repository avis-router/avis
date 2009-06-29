package org.avis.management.web;

import java.util.ArrayList;

import java.io.Closeable;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.avis.config.Options;
import org.avis.router.Router;
import org.avis.util.LogFailTester;

import static java.net.HttpURLConnection.HTTP_OK;

import static org.junit.Assert.assertEquals;

import static org.avis.logging.Log.alarm;

public class JUTestWebManagement
{
  private static final int PORT1 = 29170;
  private ArrayList<Closeable> autoClose;
  private LogFailTester logTester;

  @Before
  public void setup ()
  {
    autoClose = new ArrayList<Closeable> ();
    
    logTester = new LogFailTester ();
  }
  
  @After
  public void tearDown ()
    throws Exception
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

    logTester.assertOkAndDispose ();
  }
  
  @Test
  public void runManager () 
    throws Exception
  {
    Options options = new Options (WebManagementOptionSet.OPTION_SET);
    
    URL webURL = new URL ("http://127.0.0.1:" + (PORT1 + 1));
    
    options.set ("WebManagement.Listen", webURL.toString ());
    
    Router router = new Router (PORT1);
    
    autoClose.add (router);
    
    WebManagementManager manager = 
      new WebManagementManager (router.ioManager (), options);
    
    HttpURLConnection connection = (HttpURLConnection)webURL.openConnection ();
    
    assertEquals (HTTP_OK, connection.getResponseCode ());
    
    connection.disconnect ();
    
    autoClose.add (manager);
  }
}
