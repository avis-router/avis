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
    
    options.setAll (props);
    
    assertEquals (true, options.getBoolean ("Federation.Activated"));
    assertEquals ("require ('Message')", 
                  options.getString ("Federation.Subscribe:External"));
  }
}
