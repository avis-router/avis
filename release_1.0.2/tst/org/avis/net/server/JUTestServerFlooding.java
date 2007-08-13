package org.avis.net.server;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static dsto.dfc.logging.Log.DIAGNOSTIC;
import static dsto.dfc.logging.Log.TRACE;
import static dsto.dfc.logging.Log.info;
import static dsto.dfc.logging.Log.setEnabled;
import static dsto.dfc.logging.Log.warn;

import static java.lang.Thread.sleep;

import static org.avis.net.server.JUTestServer.PORT;

/**
 * Tests for the server's robustness to clients spamming with large
 * numbers of big messages. By default a local server is started, but
 * the tests work best with serevr started in a separate VM. Set
 * USE_EXTERNAL_SERVER to true if you want to do this and run a server
 * on port 29170.
 */
public class JUTestServerFlooding
{
  private static final boolean USE_EXTERNAL_SERVER = false;

  /** Time in millis to run flood tests. */
  private static final long FLOODING_TIME = 10 * 1000;
  
  private Server server;

  @Before
  public void startServer ()
    throws IOException
  {
    if (!USE_EXTERNAL_SERVER)
      server = new Server (PORT);
  }
  
  @After
  public void tearDown ()
  {
    if (server != null)
      server.close ();
  }
  
  /**
   * A "bad" client sends a continuous flood of large messages while
   * three others try to exchange messages. This test doesn't actually
   * assert anything, but simply tests whether server can keep serving
   * while being flooded.
   */
  @Test
  @Ignore
  public void floodingFairness ()
    throws Exception
  {
    setEnabled (TRACE, false);
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
    
    sleep (FLOODING_TIME);
    
    badClient.stopFlooding ();
    goodClient1.stopSending ();
    goodClient2.stopSending ();    
    goodClient3.stopSending ();    
    
    try
    {
      badClient.close (20000);
    } catch (MessageTimeoutException ex)
    {
      warn ("Bad client close () failed: " + ex.getMessage (), this);
    }
    
    goodClient1.close (10000);
    goodClient2.close (10000);
    goodClient3.close (10000);
    
    info (badClient.report (), this);
    info (goodClient1.report (), this);
    info (goodClient2.report (), this);
    info (goodClient3.report (), this);
  }
  
  /**
   * Try to blow server's heap by setting up a number of "bad" clients
   * all spamming large messages at server.
   */
  @Test
  @Ignore
  public void floodingHeap ()
    throws Exception
  {
    setEnabled (TRACE, false);
    setEnabled (DIAGNOSTIC, false);
    
    List<MaliciousClient> badClients = new ArrayList<MaliciousClient> ();
    
    for (int i = 0; i < 4; i++)
      badClients.add (new MaliciousClient ("Bad client " + i, "localhost", PORT));
    
    for (MaliciousClient client : badClients)
      client.startFlooding ();

    info ("Waiting while clients do their thing...", this);
    
    sleep (FLOODING_TIME);
    
    for (MaliciousClient client : badClients)
      client.stopFlooding ();
    
    for (MaliciousClient client : badClients)
      client.report ();
    
    info ("Closing clients...", this);
    
    // close () can take a long time when queues backed up...
    for (MaliciousClient client : badClients)
      client.close (30000);
    
    info ("Done", this);
  }
}