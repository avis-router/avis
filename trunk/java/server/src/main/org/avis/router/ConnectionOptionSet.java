package org.avis.router;

import org.avis.config.OptionSet;

import static org.avis.common.Common.K;
import static org.avis.common.Common.MAX;
import static org.avis.common.Common.MB;

/**
 * Defines Avis client connection options.
 * <p>
 * 
 * From Sec 7.5:
 * 
 * <pre>
 *      Name                        |  Type    |  Min   Default      Max
 *      ----------------------------+----------+-------------------------
 *      Attribute.Max-Count         |  int32   |    64     256     2**31
 *      Attribute.Name.Max-Length   |  int32   |    64    2048     2**31
 *      Attribute.Opaque.Max-Length |  int32   |    1K      1M     2**31
 *      Attribute.String.Max-Length |  int32   |    1K      1M     2**31
 *      Packet.Max-Length           |  int32   |
 *      Receive-Queue.Drop-Policy   |  string  |
 *      Receive-Queue.Max-Length    |  int32   |
 *      Send-Queue.Drop-Policy      |  string  |
 *      Send-Queue.Max-Length       |  int32   |
 *      Subscription.Max-Count      |  int32   |
 *      Subscription.Max-Length     |  int32   |
 *      Supported-Key-Schemes       |  string  |
 *      ----------------------------+----------+-------------------------
 * </pre>
 * 
 * @author Matthew Phillips
 */
public class ConnectionOptionSet extends OptionSet
{
  public static final ConnectionOptionSet CONNECTION_OPTION_SET =
    new ConnectionOptionSet ();
  
  public ConnectionOptionSet ()
  {
    // ------------ Options required for all Elvin implementations
    
    add ("Packet.Max-Length", 1*K, 2*MB, 500*MB);

    add ("Send-Queue.Max-Length", 10*K, 3*MB, 20*MB);
    
    /* We only implement Send-Queue.Drop-Policy "oldest" and "fail".
       All standard options are: "oldest", "newest", "largest", "fail". */
    add ("Send-Queue.Drop-Policy", "fail", "oldest");

    /*
     * todo: we only enforce max packet length, which by implication
     * limits the values below. The correct min, default, max values
     * are currently commented out and replaced with MAX to avoid
     * lying to clients that actually care about these.
     */
    // add ("Attribute.Max-Count", 64, 256, MAX);    
    // add ("Attribute.Name.Max-Length", 64, 2*K, MAX);
    // add ("Attribute.Opaque.Max-Length", 1*K, 1*MB, MAX);
    // add ("Attribute.String.Max-Length", 1*K, 1*MB, MAX);
    // add ("Subscription.Max-Count", 1*K, 2*K, MAX);
    // add ("Subscription.Max-Length", 1*K, 2*K, MAX);
    add ("Attribute.Max-Count", MAX, MAX, MAX);    
    add ("Attribute.Name.Max-Length", MAX, MAX, MAX);
    add ("Attribute.Opaque.Max-Length", MAX, MAX, MAX);
    add ("Attribute.String.Max-Length", MAX, MAX, MAX);
    
    add ("Subscription.Max-Count", 16, 2*K, 2*K);
    add ("Subscription.Max-Length", 1*K, 2*K, 4*K);
    
    add ("Receive-Queue.Max-Length", 0, 0, 0);

    // todo: enforce following queue-related options
    add ("Receive-Queue.Drop-Policy", "oldest");

    add ("Supported-Key-Schemes", "SHA-1");    
    
    add ("TCP.Send-Immediately", 0, 0, 1);

    // ------------ Avis-specific options
    
    // Max connection keys for ntfn/sub
    add ("Connection.Max-Keys", 0, 1*K, 1*K);
    add ("Subscription.Max-Keys", 0, 256, 1*K);
  }
}
