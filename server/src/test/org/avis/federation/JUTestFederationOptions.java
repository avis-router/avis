package org.avis.federation;

import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JUTestFederationOptions
{
  @Test
  public void basic () 
    throws Exception
  {
    FederationOptions options = new FederationOptions ();
    
    Properties props = new Properties ();
    
    props.setProperty ("Federation.Activated", "Yes");
    props.setProperty ("Federation.Subscribe:Internal", "TRUE");
    props.setProperty ("Federation.Subscribe:External", "require ('Message')");
    props.setProperty ("Federation.Provide:Internal", "TRUE");
    props.setProperty ("Federation.Connect:Internal", "ewaf://localhost");
    props.setProperty ("Federation.Connect:External", "ewaf://public.elvin.org");
    props.setProperty ("Federation.Listen:External", ".elvin.org");
    props.setProperty ("Federation.Listen", "ewaf://0.0.0.0");
    
    options.setAll (props);
    
    assertEquals (true, options.getBoolean ("Federation.Activated"));
    assertEquals ("ewaf://0.0.0.0", options.get ("Federation.Listen"));
    assertEquals (".elvin.org", options.get ("Federation.Listen:External"));
    assertEquals ("require ('Message')", 
                  options.getString ("Federation.Subscribe:External"));
  }
}
