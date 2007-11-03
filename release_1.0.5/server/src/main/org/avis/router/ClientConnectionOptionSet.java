package org.avis.router;

import java.util.HashMap;
import java.util.Map;

import org.avis.config.Options;
import org.avis.util.IllegalOptionException;

/**
 * Extends the Avis connection option set to be used against the
 * options sent by router clients. Adds code to support legacy
 * compatibility and make value checking stricter.
 * <p>
 * 
 * From Sec 7.5:
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
public class ClientConnectionOptionSet extends ConnectionOptionSet
{
  public static final ClientConnectionOptionSet CLIENT_CONNECTION_OPTION_SET =
    new ClientConnectionOptionSet ();
  
  private Map<String, String> legacyToNew;
  private Map<String, String> newToLegacy;
  
  public ClientConnectionOptionSet ()
  {
    this.legacyToNew = new HashMap<String, String> ();
    this.newToLegacy = new HashMap<String, String> ();
    
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
   * removes auto value conversion.
   */
  @Override
  protected void validateAndSet (Options options,
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
    
    // validate and set, or simply discard value
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
