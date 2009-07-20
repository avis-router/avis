package org.avis.management;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.avis.config.Options;
import org.avis.management.ManagementManager;
import org.avis.management.ManagementOptionSet;
import org.avis.router.Router;
import org.avis.util.AutoClose;
import org.avis.util.LogFailTester;

import static java.net.HttpURLConnection.HTTP_OK;

import static org.junit.Assert.assertEquals;

public class JUTestManagement
{
  private static final int PORT1 = 29170;
  
  private AutoClose autoClose;
  private LogFailTester logTester;

  @Before
  public void setup ()
  {
    autoClose = new AutoClose ();
    logTester = new LogFailTester ();
  }
  
  @After
  public void tearDown ()
    throws Exception
  {
    autoClose.close ();
    logTester.assertOkAndDispose ();
  }
  
  @Test
  public void runManager () 
    throws Exception
  {
    Options options = new Options (ManagementOptionSet.OPTION_SET);
    
    URL webURL = new URL ("http://127.0.0.1:" + (PORT1 + 1));
    
    options.set ("Management.Listen", webURL.toString ());
    
    Router router = new Router (PORT1);
    
    autoClose.add (router);
    
    ManagementManager manager = new ManagementManager (router, options);
    
    HttpURLConnection connection = (HttpURLConnection)webURL.openConnection ();
    
    assertEquals (HTTP_OK, connection.getResponseCode ());
    
    connection.disconnect ();
    
    autoClose.add (manager);
  }  
}
