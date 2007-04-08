package org.avis.net.common;

import java.util.HashMap;
import java.util.Map;

import org.avis.net.server.ConnectionOptions;

import org.junit.Test;

import static org.avis.net.server.ConnectionOptionSet.CONNECTION_OPTION_SET;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test the {@link ConnectionOptions} class.
 * 
 * @author Matthew Phillips
 */
public class JUTestConnectionOptions
{
  @Test
  public void validation ()
  {
    HashMap<String, Object> requested = new HashMap<String, Object> ();
    
    requested.put ("Packet.Max-Length", 8 * 1024 * 1024); // valid
    requested.put ("Attribute.Max-Count", 63); // bogus value
    requested.put ("Bogus", 42); // bogus
    requested.put ("Receive-Queue.Drop-Policy", "bogus"); // bogus
    requested.put ("Send-Queue.Drop-Policy", "fail"); // valid
    requested.put ("Receive-Queue.Drop-Policy", "oldest"); // valid, default
    requested.put ("Send-Queue.Max-Length", "bogus"); // bogus
    
    ConnectionOptions options = new ConnectionOptions (requested);
    Map<String, Object> valid = options.accepted ();
    
    assertRequested (requested, valid, "Packet.Max-Length");
    assertDefault (valid, "Attribute.Max-Count");
    assertNull (valid.get ("Bogus"));
    assertDefault (valid, "Receive-Queue.Drop-Policy");
    assertRequested (requested, valid, "Send-Queue.Drop-Policy");
    assertRequested (requested, valid, "Receive-Queue.Drop-Policy");
    assertDefault (valid, "Send-Queue.Max-Length");
  }
  
  @Test
  public void compatibility ()
  {
    HashMap<String, Object> requested = new HashMap<String, Object> ();
    
    requested.put ("router.attribute.string.max-length", 8 * 1024 * 1024); // valid
    requested.put ("router.send-queue.drop-policy", "fail"); // valid
    requested.put ("router.attribute.max-count", 63); // bogus
    requested.put ("router.coalesce-delay", 0); // valid
    
    ConnectionOptions options = new ConnectionOptions (requested);
    Map<String, Object> accepted = options.accepted ();
    
    // todo: when Attribute.String.Max-Length supported, switch lines below
    // assertRequested (requested, valid, "router.attribute.string.max-length");
    assertDefault (accepted, "router.attribute.string.max-length");
    assertDefault (accepted, "router.attribute.max-count");
    assertRequested (requested, accepted, "router.send-queue.drop-policy");
    assertRequested (requested, accepted, "router.coalesce-delay");
  }

  private static void assertDefault (Map<String, Object> accepted,
                                     String name)
  {
    assertEquals
      (CONNECTION_OPTION_SET.defaults.get
        (CONNECTION_OPTION_SET.legacyToNew (name)), accepted.get (name));
  }

  private static void assertRequested (Map<String, Object> requested,
                                       Map<String, Object> valid,
                                       String name)
  {
    assertEquals (requested.get (name), valid.get (name));
  }
}
