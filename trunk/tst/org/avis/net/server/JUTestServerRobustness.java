package org.avis.net.server;

import org.junit.After;
import org.junit.Test;

import static dsto.dfc.logging.Log.DIAGNOSTIC;
import static dsto.dfc.logging.Log.TRACE;
import static dsto.dfc.logging.Log.info;
import static dsto.dfc.logging.Log.setEnabled;

import static java.lang.Thread.sleep;

import static org.avis.net.server.JUTestServer.PORT;

public class JUTestServerRobustness
{
  private Server server;

  @After
  public void tearDown ()
  {
    if (server != null)
      server.close ();
  }
  
  /**
   * A bad client sends a continuous flood of large messages while
   * three others try to exchange messages.
   */
  @Test
  public void flooding ()
    throws Exception
  {
    setEnabled (TRACE, false);

    server = new Server (PORT);
    setEnabled (DIAGNOSTIC, false);
    
    MaliciousClient badClient = new MaliciousClient ("Bad client", "localhost", PORT);
    GoodClient goodClient1 = new GoodClient ("Good client 1", "localhost", PORT);
    GoodClient goodClient2 = new GoodClient ("Good client 2", "localhost", PORT);
    GoodClient goodClient3 = new GoodClient ("Good client 3", "localhost", PORT);
    
    badClient.startFlooding ();
    goodClient1.startSending ();
    goodClient2.startSending ();
    goodClient3.startSending ();
    
    info ("Waiting while clients do their thing...", this);
    
    sleep (5000);
    
    badClient.stopFlooding ();
    goodClient1.stopSending ();
    goodClient2.stopSending ();    
    goodClient3.stopSending ();    
    
    badClient.close ();
    goodClient1.close ();
    goodClient2.close ();
    goodClient3.close ();
    
    info (badClient.report (), this);
    info (goodClient1.report (), this);
    info (goodClient2.report (), this);
    info (goodClient3.report (), this);
  }
}