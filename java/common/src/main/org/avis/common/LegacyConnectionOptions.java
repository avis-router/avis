package org.avis.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines legacy client connection option compatibility. Provides a
 * two-way mapping between old-style connection options and the new
 * scheme defined in the 4.0 client specification.
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
 */
public final class LegacyConnectionOptions
{
  private static final Map<String, String> legacyToNew;
  private static final Map<String, String> newToLegacy;

  static
  {
    legacyToNew = new HashMap<String, String> ();
    newToLegacy = new HashMap<String, String> ();
    
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

  private LegacyConnectionOptions ()
  {
    // zip
  }
  
  private static void addLegacy (String oldOption, String newOption)
  {
    legacyToNew.put (oldOption, newOption);
    newToLegacy.put (newOption, oldOption);
  }

  public static String legacyToNew (String option)
  {
    if (legacyToNew.containsKey (option))
      return legacyToNew.get (option);
    else
      return option;
  }

  public static String newToLegacy (String option)
  {
    if (newToLegacy.containsKey (option))
      return newToLegacy.get (option);
    else
      return option;
  }
}
