package org.avis.client;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import static java.util.Collections.unmodifiableMap;

import static org.avis.client.ConnectionOptions.convertLegacyToNew;
import static org.avis.common.LegacyConnectionOptions.newToLegacy;
import static org.junit.Assert.assertEquals;

public class JUTestLegacyConnectionOptions
{
  /**
   * Test connecting to a Mantara router with a new-style option.
   */
  @Ignore
  @Test
  public void connectMantara ()
    throws Exception
  {
    ConnectionOptions options = new ConnectionOptions ();
    options.set ("TCP.Send-Immediately", 1);
    
    Elvin elvin = new Elvin ("elvin://elvin", options);
    
    assertEquals (1, elvin.connectionOptions ().get ("TCP.Send-Immediately"));
    
    elvin.close ();
  }
  
  @Test
  public void connectLegacy ()
    throws Exception
  {
    ConnectionOptions options = new ConnectionOptions ();
    options.includeLegacy (true);
    
    options.set ("bogus", 1);
    options.set ("Packet.Max-Length", 1024);
    options.set ("TCP.Send-Immediately", 1);
    
    Map<String, Object> optionsWithLegacy = options.asMapWithLegacy ();
    
    assertEquals (map 
                   ("bogus", 1,
                    "Packet.Max-Length", 1024,
                    newToLegacy ("Packet.Max-Length"), 1024,
                    "TCP.Send-Immediately", 1,
                    newToLegacy ("TCP.Send-Immediately"), 0),
                  optionsWithLegacy);
    
    Map<String, Object> optionsWithoutLegacy = 
      convertLegacyToNew (optionsWithLegacy);
    
    assertEquals (map 
                   ("bogus", 1,
                    "Packet.Max-Length", 1024,
                    "TCP.Send-Immediately", 1),
                  optionsWithoutLegacy);
    
    assertEquals (map 
                   ("TCP.Send-Immediately", 0,
                    "Attribute.Max-Count", 10,
                    "Packet.Max-Length", 1024),
                  convertLegacyToNew (map 
                   ("router.coalesce-delay", 1,
                    "router.attribute.max-count", 10,
                    "router.packet.max-length", 1024)));
  }
  
  private Map<String, Object> map (Object... pairs)
  {
    if (pairs.length % 2 != 0)
      throw new IllegalArgumentException ("Items must be a set of pairs");
    
    HashMap<String, Object> map = new HashMap<String, Object> ();
    
    for (int i = 0; i < pairs.length; i += 2)
      map.put ((String)pairs [i], pairs [i + 1]);

    return unmodifiableMap (map);
  }
}
