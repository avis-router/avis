package org.avis.router;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

import org.apache.mina.core.buffer.IoBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.avis.util.LogFailTester;

import static java.lang.Thread.sleep;

import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.enableLogging;
import static org.avis.logging.Log.info;
import static org.avis.logging.Log.warn;
import static org.avis.router.JUTestRouter.PORT;

/**
 * Tests for the server's robustness to clients spamming with large
 * numbers of big messages. By default a local server is started, but
 * the tests work best with serevr started in a separate VM. Set
 * USE_EXTERNAL_SERVER to true if you want to do this and run a server
 * on port 29170.
 */
public class JUTestFlooding
{
  private static final boolean USE_EXTERNAL_SERVER = false;

  /** Time in millis to run flood tests. */
  private static final long FLOODING_TIME = 20 * 1000;
  
  private Router server;
  private LogFailTester logTester;

  @Before
  public void setup ()
    throws IOException
  {
    if (!USE_EXTERNAL_SERVER)
    {   
      RouterOptions options = new RouterOptions ();
      options.set ("Port", PORT);
      // options.set ("IO.Use-Direct-Buffers", false);
      
      server = new Router (options);
    }
    
    logTester = new LogFailTester ();
    
    IoBuffer.setUseDirectBuffer (false);
  }
  
  @After
  public void shutdown ()
  {
    if (server != null)
      server.close ();
    
    logTester.assertOkAndDispose ();
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
    enableLogging (TRACE, false);
    enableLogging (DIAGNOSTIC, false);
    
    List<GoodClient> goodClients = new ArrayList<GoodClient> ();
    List<MaliciousClient> badClients = new ArrayList<MaliciousClient> ();
    
    for (int i = 1; i <= 1; i++)
      badClients.add (new MaliciousClient ("Bad client " + i, "localhost", PORT));

    for (int i = 1; i <= 4; i++)
      goodClients.add (new GoodClient ("Good client " + i, "localhost", PORT));

    for (MaliciousClient badClient : badClients)
      badClient.startFlooding ();      

    for (GoodClient goodClient : goodClients)
      goodClient.startSending ();

    info ("Waiting while clients do their thing...", this);
    
    sleep (FLOODING_TIME);
    
    for (MaliciousClient badClient : badClients)
      badClient.stopFlooding ();      

    for (GoodClient goodClient : goodClients)
      goodClient.stopSending ();
    
    for (MaliciousClient badClient : badClients)
    {
      try
      {
        badClient.close (20000);
      } catch (MessageTimeoutException ex)
      {
        warn ("Bad client close () failed: " + ex.getMessage (), this);
      }
    }
    
    for (GoodClient goodClient : goodClients)
      goodClient.close (10000);

    for (MaliciousClient badClient : badClients)
      info (badClient.report (), this);

    for (GoodClient goodClient : goodClients)
      info (goodClient.report (), this);
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
    enableLogging (TRACE, false);
    enableLogging (DIAGNOSTIC, false);
    
    List<MaliciousClient> badClients = new ArrayList<MaliciousClient> ();
    
    for (int i = 0; i < 20; i++)
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
      client.close (60000);
    
    info ("Done", this);
  }
}