package org.avis.net.server;

import java.util.HashMap;
import java.util.Map;

import org.avis.util.IllegalOptionException;
import org.avis.util.OptionSet;
import org.avis.util.Options;

import static org.avis.common.Common.K;
import static org.avis.common.Common.MAX;
import static org.avis.common.Common.MB;

/**
 * Defines the Avis connection option set: definition, validation and legacy
 * compatibility.
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
 * Compatibility:
 * 
 * <pre>
 *      Standard Name               | Compatibility Name
 *      ----------------------------+------------------------------------
 *      Attribute.Max-Count         | router.attribute.max-count
 *      Attribute.Name.Max-Length   | router.attribute.name.max-length
 *      Attribute.Opaque.Max-Length | router.attribute.opaque.max-length
 *      Attribute.String.Max-Length | router.attribute.string.max-length
 *      Packet.Max-Length           | router.packet.max-length
 *      Receive-Queue.Drop-Policy   | router.recv-queue.drop-policy
 *      Receive-Queue.Max-Length    | router.recv-queue.max-length
 *      Send-Queue.Drop-Policy      | router.send-queue.drop-policy
 *      Send-Queue.Max-Length       | router.send-queue.max-length
 *      Subscription.Max-Count      | router.subscription.max-count
 *      Subscription.Max-Length     | router.subscription.max-length
 *      Supported-Key-Schemes       | router.supported-keyschemes
 *      Vendor-Identification       | router.vendor-identification
 *      ----------------------------+------------------------------------
 * </pre>
 * 
 * @author Matthew Phillips
 */
public class ConnectionOptionSet extends OptionSet
{
  public static final ConnectionOptionSet CONNECTION_OPTION_SET =
    new ConnectionOptionSet ();
  
  private Map<String, String> legacyToNew;
  private Map<String, String> newToLegacy;

  public ConnectionOptionSet ()
  {
    this.legacyToNew = new HashMap<String, String> ();
    this.newToLegacy = new HashMap<String, String> ();
    
    // ------------ Options required for all Elvin implementations
    
    add ("Packet.Max-Length", 1*K, 1*MB, 10*MB);
    
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
    
    add ("Receive-Queue.Max-Length", 1*K, 1*MB, 1*MB);

    // todo: enforce following queue-related options
    add ("Receive-Queue.Drop-Policy",
         "oldest", "newest", "largest", "fail");
    
    add ("Send-Queue.Drop-Policy",
         "oldest", "newest", "largest", "fail");
    add ("Send-Queue.Max-Length", MAX, MAX, MAX);

    add ("Supported-Key-Schemes", "SHA-1");    
    
    add ("TCP.Send-Immediately", 0, 0, 1);

    // ------------ Avis-specific options
    
    // Max connection keys for ntfn/sub
    add ("Connection.Max-Keys", 0, 1*K, 1*K);
    add ("Subscription.Max-Keys", 0, 256, 1*K);
    
    // ------------ Legacy options
    
    addLegacy ("router.attribute.max-count", "Attribute.Max-Count");
    addLegacy ("router.attribute.name.max-length",
               "Attribute.Name.Max-Length");
    addLegacy ("router.attribute.opaque.max-length",
               "Attribute.Opaque.Max-Length");
    addLegacy ("router.attribute.string.max-length",
               "Attribute.String.Max-Length");
    addLegacy ("router.packet.max-length", "Packet.Max-Length");
    addLegacy ("router.recv-queue.drop-policy",
               "Receive-Queue.Drop-Policy");
    addLegacy ("router.recv-queue.max-length",
               "Receive-Queue.Max-Length");
    addLegacy ("router.send-queue.drop-policy",
               "Send-Queue.Drop-Policy");
    addLegacy ("router.send-queue.max-length",
               "Send-Queue.Max-Length");
    addLegacy ("router.subscription.max-count",
               "Subscription.Max-Count");
    addLegacy ("router.subscription.max-length",
               "Subscription.Max-Length");
    addLegacy ("router.supported-keyschemes", "Supported-Key-Schemes");
    addLegacy ("router.vendor-identification",
               "Vendor-Identification");
    addLegacy ("router.coalesce-delay",
               "TCP.Send-Immediately");
  }
  
  private void addLegacy (String oldOption, String newOption)
  {
    legacyToNew.put (oldOption, newOption);
    newToLegacy.put (newOption, oldOption);
  }
  
  /**
   * Generate the set of accepted connection options for reporting to
   * a client. Handles removal of invalid options, echoing of actual
   * defaults for values out of range and legacy backward
   * compatibility.
   * 
   * @param connectionOptions The connection option set.
   * @param requestedOptions The original requested set of options.
   * 
   * @return The accepted set suitable for reporting to client as per
   *         the client connection option spec.
   */
  public Map<String, Object> accepted (Options connectionOptions,
                                       Map<String, Object> requestedOptions)
  {
    HashMap<String, Object> accepted = new HashMap<String, Object> ();
    
    for (String requestedOption : requestedOptions.keySet ())
    {
      String option = legacyToNew (requestedOption);
      
      if (isDefined (option))
      {
        Object value = connectionOptions.peek (option);

        if (value == null)
          value = defaults.get (option);

        /*
         * Special handling for old router.coalesce-delay, which has the
         * opposite meaning to its replacement, TCP.Send-Immediately.
         */
        if (requestedOption.equals ("router.coalesce-delay"))
        {
          if (value.equals (0))
            value = 1;
          else if (value.equals (1))
            value = 0;
        }
        
        accepted.put (requestedOption, value);
      }
    }
    
    return accepted;
  }

  /**
   * Override validation to add legacy support and to simply not
   * include invalid options rather than explode violently. Also
   * removes auto value conversion, since we should treat mismatched
   * values as invalid.
   */
  @Override
  protected void validateAndPut (Options options,
                                 String option, Object value)
    throws IllegalOptionException
  {
    /*
     * Special handling for old router.coalesce-delay, which has the
     * opposite meaning to its replacement, TCP.Send-Immediately.
     */
    if (option.equals ("router.coalesce-delay"))
    {
      if (value.equals (0))
        value = 1;
      else if (value.equals (1))
        value = 0;
    }
    
    option = legacyToNew (option);
    
    if (validate (option, value) == null)
      set (options, option, value);
  }

  public String legacyToNew (String option)
  {
    if (legacyToNew.containsKey (option))
      return legacyToNew.get (option);
    else
      return option;
  }

  public String newToLegacy (String option)
  {
    if (newToLegacy.containsKey (option))
      return newToLegacy.get (option);
    else
      return option;
  }
}
