package org.avis.net.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.Collections.unmodifiableMap;

import static org.avis.util.Util.valuesEqual;

/**
 * Connection options sent by the client to the server.
 * 
 * <h2>Standard Elvin connection options</h2>
 * 
 * <p>See http://elvin.org/specs for details on client connection options.</p>
 * 
 * Packet.Max-Length. Max packet length acceptable from a client.
 * <p>
 * Subscription.Max-Count. Maximum number of subscriptions allowed by a
 * single client.
 * <p>
 * Subscription.Max-Length. Maximum length, in bytes, of any subscription
 * expression.
 * <p>
 * Receive-Queue.Max-Length. The maximum size of the router's per-client
 * incoming packet queue, in bytes. If the queue exceeds this size, the
 * router will throttle the data stream from the client until the queue
 * drops below this value.
 * <p>
 * TCP.Send-Immediately. Set whether the TCP NO_DELAY flag is enabled for
 * sockets on the server side. 1 = send immediately (TCP NO_DELAY =
 * true), 0 = do not necessarily send immediately, buffer data for
 * optimal throughput (TCP NO_DELAY = false). Set this to 1 if you
 * experience lag with "real time" applications that require minimal
 * delivery latency, but note that this may result in an overall
 * reduction in throughput.
 * <p>
 * Attribute.Name.Max-Length. Attribute.Max-Count. The maximum number of
 * attributes on a notification.
 * <p>
 * Attribute.Opaque.Max-Length. Maximum length, in bytes, for opaque
 * values.
 * <p>
 * Attribute.String.Max-Length. Maximum length, in bytes, for opaque
 * values.  Note that this value is not the number of characters: some
 * characters may take up to 5 bytes to respresent using the require
 * UTF-8 encoding.
 * <p>
 * Receive-Queue.Drop-Policy. This property describes the desired
 * behaviour of the router's packet receive queue if it exceeds the
 * negotitated maximum size. Values: "oldest", "newest", "largest",
 * "fail"
 * <p>
 * Send-Queue.Drop-Policy. This property describes the desired behaviour
 * of the router's packet send queue if it exceeds the negotitated
 * maximum size. Values: "oldest", "newest", "largest", "fail"
 * <p>
 * Send-Queue.Max-Length. The maximum length (in bytes) of the routers
 * send queue. 
 * 
 * @author Matthew Phillips
 */
public class ConnectionOptions
{
  private HashMap<String, Object> values;

  public ConnectionOptions ()
  {
    this.values = new HashMap<String, Object> ();
  }
  
  public void set (String name, Object value)
  {
    values.put (name, value);
  }
  
  public Object get (String name)
  {
    Object value = values.get (name);
    
    if (value == null)
      throw new IllegalArgumentException ("No value for \"" + name + "\"");
    else
      return value;
  }
  
  /**
   * Generate the difference between this option set and an actual set
   * returned by the server.
   */
  protected Map<String, Object> differenceFrom (Map<String, Object> options)
  {
    HashMap<String, Object> diff = new HashMap<String, Object> ();
    
    for (Entry<String, Object> entry : values.entrySet ())
    {
      Object actualValue = options.get (entry.getKey ());
      
      if (!valuesEqual (entry.getValue (), actualValue))
        diff.put (entry.getKey (), actualValue);
    }
    
    return diff;
  }

  public Map<String, Object> asMap ()
  {
    return unmodifiableMap (values);
  }
}
