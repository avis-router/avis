package org.avis.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import static org.avis.Common.K;
import static org.avis.Common.MB;

import static org.junit.Assert.assertEquals;

public class JUTestOptions
{
  @Test
  public void basic ()
  {
    ConnectionOptionSet connectionOptionSet = new ConnectionOptionSet ();
    OptionSet routerOptionSet = new OptionSet (connectionOptionSet);
    
    routerOptionSet.add ("Port", 1, 2917, 65535);
    routerOptionSet.add ("Vendor-Identification", "Foobar");
    
    Options routerOptions = new Options (routerOptionSet);
    
    Properties loadedProperties = new Properties ();
    loadedProperties.setProperty ("Port", "29170");
    loadedProperties.setProperty ("Keys.Max-Count", "42");
    
    routerOptions.setAll (loadedProperties);
    
    Map<String, Object> requestedOptions = new HashMap<String, Object> ();
    requestedOptions.put ("Packet.Max-Length", 20*K);
    requestedOptions.put ("router.subscription.max-count", 1234);
    
    Options connectionOptions = new Options (connectionOptionSet);
    connectionOptions.addDefaults (routerOptions);
    connectionOptions.setAll (requestedOptions);
    
    Map<String, Object> accepted =
      connectionOptionSet.accepted (connectionOptions, requestedOptions);
    
    assertEquals (29170, routerOptions.getInt ("Port"));
    assertEquals (20*K, connectionOptions.getInt ("Packet.Max-Length"));
    assertEquals (42, connectionOptions.getInt ("Keys.Max-Count"));
    assertEquals (1234, connectionOptions.getInt ("Subscription.Max-Count"));
    assertEquals (1234, accepted.get ("router.subscription.max-count"));
  }
  
  static class ConnectionOptionSet extends OptionSet
  {
    private Map<String, String> legacyToNew;
    private Map<String, String> newToLegacy;

    public ConnectionOptionSet ()
    {
      this.legacyToNew = new HashMap<String, String> ();
      this.newToLegacy = new HashMap<String, String> ();
      
      add ("Packet.Max-Length", 1*K, 1*MB, 1*MB);
      add ("Subscription.Max-Count", 16, 2*K, 2*K);
      add ("Keys.Max-Count", 16, 2*K, 2*K);
      add ("Send-Queue.Drop-Policy",
           "oldest", "newest", "largest", "fail");
      
      addLegacy ("router.packet.max-length", "Packet.Max-Length");
      addLegacy ("router.subscription.max-count",
                 "Subscription.Max-Count");
    }

    public Map<String, Object> accepted (Options connectionOptions,
                                         Map<String, Object> requestedOptions)
    {
      HashMap<String, Object> accepted = new HashMap<String, Object> ();
      
      for (String option : connectionOptions.options ())
      {
        Object value = connectionOptions.get (option);
        
        if (!requestedOptions.containsKey (option))
          option = newToLegacy.get (option);
        
        accepted.put (option, value);
      }
      
      return accepted;
    }

    private void addLegacy (String oldOption, String newOption)
    {
      legacyToNew.put (oldOption, newOption);
      newToLegacy.put (newOption, oldOption);
    }
    
    @Override
    protected void validateAndPut (Map<String, Object> values,
                                   String option, Object value)
      throws IllegalOptionException
    {
      if (legacyToNew.containsKey (option))
        option = legacyToNew.get (option);
      
      if (validate (option, value) == null)
        values.put (option, value);
    }
  }
}
