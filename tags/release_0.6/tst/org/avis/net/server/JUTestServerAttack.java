package org.avis.net.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import java.io.IOException;

import org.avis.net.ConnectionOptions;
import org.avis.net.messages.SubModRqst;
import org.avis.net.messages.SubRply;
import org.avis.net.security.Key;
import org.avis.net.security.Keys;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static dsto.dfc.logging.Log.DIAGNOSTIC;
import static dsto.dfc.logging.Log.TRACE;
import static dsto.dfc.logging.Log.info;
import static dsto.dfc.logging.Log.setEnabled;

import static org.avis.net.security.KeyScheme.SHA1_CONSUMER;
import static org.avis.net.server.JUTestServer.PORT;

/**
 * Test attacks deliberately designed to DOS/crash the server by
 * exhausting resources.
 * 
 * @author Matthew Phillips
 */
public class JUTestServerAttack
{
  private Random random;
  private Server server;

  @Before
  public void setup ()
    throws IOException
  {
    setEnabled (TRACE, false);
    setEnabled (DIAGNOSTIC, false);
    
    random = new Random ();
    server = new Server (PORT);
  }
  
  @After
  public void teardown ()
  {
    if (server != null)
      server.close ();
  }
  
  /**
   * Attack the server's heap space by adding unlimited numbers of
   * keys. Without key limits, takes around 3 mins to exhaust server's
   * heap space on Powerbook G4.
   */
  @Test
  public void attackKeys ()
    throws Exception
  {
    SimpleClient client = new SimpleClient ("localhost", PORT);
    
    client.connect ();
    
    SubRply subRply = client.subscribe ("number == 1");
    
    info ("Sending keys...", this);
    
    for (int i = 0; i < 10000; i++)
    {
      Keys keys = new Keys ();
      
      for (int j = 0; j < 2000; j++)
        keys.add (SHA1_CONSUMER, new Key (randomBytes (128)));
      
      SubModRqst subMod = new SubModRqst (subRply.subscriptionId, "");
      subMod.addKeys = keys;
      
      client.send (subMod);
      client.receive (60 * 1000);
    }
    
    client.close ();
  }

  /**
   * Attack server by sending the max number of subs with very long
   * expressions. Takes about 2 mins to kill server on Powerbook G4.
   */
  @Test
  public void attackSubscriptions ()
    throws Exception
  {
    int maxSubs = ConnectionOptions.getMaxValue ("Subscription.Max-Count");
    int maxLength = ConnectionOptions.getMaxValue ("Subscription.Max-Length");

    SimpleClient client = new SimpleClient ("localhost", PORT);
    
    Map<String, Object> options = new HashMap<String, Object> ();
    options.put ("Subscription.Max-Count", maxSubs);
    options.put ("Subscription.Max-Length", maxLength);
    
    client.connect (options);
    
    info ("Subscribing...", this);
    
    String subscriptionExpr = dummySubscription (maxLength);
    
    for (int i = 0; i < maxSubs; i++)
      client.subscribe (subscriptionExpr);
    
    client.close ();
  }

  private static String dummySubscription (int length)
  {
    StringBuilder str = new StringBuilder ("i == -1");
    
    for (int i = 0; str.length () + 15 < length; i++)
      str.append (" && i == " + i);
    
    return str.toString ();
  }

  private byte [] randomBytes (int length)
  {
    byte [] data = new byte [length];
    
    random.nextBytes (data);
    
    return data;
  }
}
