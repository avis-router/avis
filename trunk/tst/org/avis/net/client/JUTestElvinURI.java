package org.avis.net.client;

import java.net.URISyntaxException;

import org.junit.Test;

import static org.avis.Common.CLIENT_VERSION_MAJOR;
import static org.avis.Common.CLIENT_VERSION_MINOR;
import static org.avis.Common.DEFAULT_PORT;
import static org.avis.util.Collections.list;
import static org.avis.util.Collections.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class JUTestElvinURI
{
  @Test
  public void version ()
    throws URISyntaxException
  {
    ElvinURI uri = new ElvinURI ("elvin://elvin_host");
    
    assertEquals (CLIENT_VERSION_MAJOR, uri.versionMajor);
    assertEquals (CLIENT_VERSION_MINOR, uri.versionMinor);
    assertEquals ("elvin_host", uri.host);
    
    uri = new ElvinURI ("elvin:5.1//elvin_host");
    
    assertEquals (5, uri.versionMajor);
    assertEquals (1, uri.versionMinor);  
    assertEquals ("elvin_host", uri.host);
    
    uri = new ElvinURI ("elvin:5//elvin_host");
    
    assertEquals (5, uri.versionMajor);
    assertEquals (CLIENT_VERSION_MINOR, uri.versionMinor);
    assertEquals ("elvin_host", uri.host);
    
    assertInvalid ("elvin:hello//elvin_host");
    assertInvalid ("elvin:4.0.0//elvin_host");
    assertInvalid ("elvin:4.//elvin_host");
    assertInvalid ("elvin: //elvin_host");
  }

  @Test
  public void protocol ()
    throws URISyntaxException
  {
    ElvinURI uri = new ElvinURI ("elvin://elvin_host");
    
    assertEquals (ElvinURI.defaultProtocol (), uri.protocol);
    
    uri = new ElvinURI ("elvin:/tcp,xdr,ssl/elvin_host");
    
    assertEquals (list ("tcp", "xdr", "ssl"), uri.protocol);
    assertEquals ("elvin_host", uri.host);
    
    uri = new ElvinURI ("elvin:/secure/elvin_host");
    
    assertEquals (ElvinURI.secureProtocol (), uri.protocol);
    
    assertInvalid ("elvin:/abc,xyz/elvin_host");
    assertInvalid ("elvin:/abc,xyz,dfg,qwe/elvin_host");
    assertInvalid ("elvin:/abc,/elvin_host");
    assertInvalid ("elvin:/,abc/elvin_host");
    assertInvalid ("elvin:/abc,,xyz/elvin_host");
    assertInvalid ("elvin:///elvin_host");
  }
  
  @Test
  public void endpoint ()
    throws URISyntaxException
  {
    ElvinURI uri = new ElvinURI ("elvin://elvin_host");
    assertEquals ("elvin_host", uri.host);
    assertEquals (DEFAULT_PORT, uri.port);
    
    uri = new ElvinURI ("elvin://elvin_host:12345");
    assertEquals ("elvin_host", uri.host);
    assertEquals (12345, uri.port);
    
    assertInvalid ("elvin://");
    assertInvalid ("elvin://hello:there");
  }
  
  @Test
  public void options ()
    throws URISyntaxException
  {
    ElvinURI uri = new ElvinURI ("elvin://elvin_host;name1=value1");
    
    assertEquals (map ("name1", "value1"), uri.options);
    
    uri = new ElvinURI ("elvin://elvin_host;name1=value1;name2=value2");
    
    assertEquals (map ("name1", "value1", "name2", "value2"), uri.options);
    
    assertInvalid ("elvin://elvin_host;name1;name2=value2");
    assertInvalid ("elvin://elvin_host;=name1;name2=value2");
    assertInvalid ("elvin://elvin_host;");
  }
  
  @Test
  public void equality ()
    throws URISyntaxException
  {
    assertSameUri ("elvin://elvin_host", "elvin://elvin_host:2917");
    assertSameUri ("elvin://elvin_host", "elvin:/tcp,none,xdr/elvin_host");
    
    assertNotSameUri ("elvin://elvin_host", "elvin:/tcp,ssl,xdr/elvin_host");
    assertNotSameUri ("elvin://elvin_host", "elvin://elvin_host:29170");
    assertNotSameUri ("elvin://elvin_host", "elvin://elvin_host;name=value");
  }
  
  private static void assertSameUri (String uri1, String uri2)
    throws URISyntaxException
  {
    assertEquals (new ElvinURI (uri1), new ElvinURI (uri2));
  }
  
  private static void assertNotSameUri (String uri1, String uri2)
    throws URISyntaxException
  {
    assertFalse (new ElvinURI (uri1).equals (new ElvinURI (uri2)));
  }

  private static void assertInvalid (String uriString)
  {
    try
    {
      new ElvinURI (uriString);
      
      fail ("Invalid URI \"" + uriString + "\" not detected");
    } catch (URISyntaxException ex)
    {
      // ok
      // System.out.println ("error = " + ex.getMessage ());
    }
  }
}
