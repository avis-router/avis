package org.avis.router;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.avis.io.messages.ConnRply;
import org.avis.io.messages.DropWarn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.avis.io.messages.Notify.asAttributes;
import static org.avis.router.JUTestRouter.PORT;

public class JUTestSendQueueThrottle
{
  /**
   * Test that router implements Send-Queue.Drop-Policy and
   * Send-Queue.Max-Length.
   */
  @Test
  public void throttle ()
    throws Exception
  {
    Router router = new Router (PORT);
    SimpleClient client = new SimpleClient ("localhost", PORT);
    
    Map<String, Object> options = new HashMap<String, Object> ();
    
    options.put ("Send-Queue.Drop-Policy", "oldest");
    options.put ("Send-Queue.Max-Length", 20*1024);
    
    ConnRply reply = client.connect (options);
    
    // check router ACK'd send queue requests
    assertEquals (options.get ("Send-Queue.Drop-Policy"), 
                  reply.options.get ("Send-Queue.Drop-Policy"));
    
    assertEquals (options.get ("Send-Queue.Max-Length"), 
                  reply.options.get ("Send-Queue.Max-Length"));
    
    // send more than 20K of messages to myself, make sure I get a DropWarn 
    client.subscribe ("To == 'me'");
    
    Map<String, Object> notification = 
      asAttributes ("To", "me", "Payload", new byte [1024]);

    for (int i = 0; i < 22; i++)
      client.sendNotify (notification);
    
    boolean dropWarned = false;
    
    for (int i = 0; i < 22 && !dropWarned; i++)
    {
      if (client.receive () instanceof DropWarn)
        dropWarned = true;
    }
 
    assertTrue ("No DropWarn", dropWarned);
    
    client.close ();
    router.close ();
  }
}
