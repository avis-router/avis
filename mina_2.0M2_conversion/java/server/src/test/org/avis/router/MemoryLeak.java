package org.avis.router;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.IoBuffer;

import org.avis.io.messages.NotifyEmit;

import static java.lang.System.currentTimeMillis;

/**
 * Send/receive a stream of notifications to the router in order to
 * watch heap activity.
 * 
 * @author Matthew Phillips
 */
public class MemoryLeak
{
  public static void main ()
    throws Exception
  {
    IoBuffer.setUseDirectBuffer (true);
    
    Router router = new Router (29170);
    SimpleClient client = new SimpleClient ();

    client.connect ();
    
    client.subscribe ("require (name)");
    
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("name", "foobar");
    ntfn.put ("data", new byte [64 * 1024]);
    
    long start = currentTimeMillis ();
    
    int count = 0;
    
    while (currentTimeMillis () - start < 30 * 60 * 1000)
    {
      client.send (new NotifyEmit (ntfn));
      
      client.receive ();
      
      count++;
      
      if ((currentTimeMillis () - start) % (10 * 1000) == 0)
      {
        System.gc ();
        System.out.println ("Heap = " + Runtime.getRuntime ().freeMemory ());
      }
    }
    
    System.out.println ("Messages = " + count);
    
    client.close ();
    router.close ();
  }
}
