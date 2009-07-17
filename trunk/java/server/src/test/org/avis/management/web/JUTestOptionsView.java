package org.avis.management.web;

import org.junit.Before;
import org.junit.Test;

import org.avis.config.Options;
import org.avis.federation.FederationOptionSet;
import org.avis.management.web.pages.OptionsView;
import org.avis.router.RouterOptionSet;

import static org.junit.Assert.assertTrue;

public class JUTestOptionsView
{
  private Options options;

  @Before
  public void setup ()
  {
    RouterOptionSet routerOptionSet = new RouterOptionSet ();
    
    routerOptionSet.inheritFrom (FederationOptionSet.OPTION_SET);
    routerOptionSet.inheritFrom (WebManagementOptionSet.OPTION_SET);
    
    options = new Options (routerOptionSet);
    
    options.set ("Port", 29170);
    
    options.set ("Federation.Connect[Public]", "ewaf://0.0.0.0");
    options.set ("Federation.Provide[Public]", "TRUE");
    options.set ("Federation.Subscribe[Public]", "require (Test)");
    options.set ("Federation.Add-Incoming-Attribute[Public][Number]", 1);
    options.set ("Federation.Add-Incoming-Attribute[Public][String]", "\"hello\"");
    options.set ("Federation.Require-Authenticated", "*.somewhere.org 111.111.111.???");
    
    options.set ("Management.Admin-Name", "admin");
    options.set ("Management.Admin-Password", "foo2");
  }
  
  @Test
  public void basic () 
    throws Exception
  {
    HTML html = new HTML ();
    
    OptionsView view = new OptionsView (options);
    
    view.render (html);
    
    String text = html.asText ();
    
    // System.out.println (text);
    
    assertContains 
      (text, "Federation.Add-Incoming-Attribute[Public][String]", "hello"); 
  }

  private void assertContains (String text, String option, Object value)
  {
    HTML html = new HTML ();
    OptionsView view = new OptionsView (options);
    
    view.renderOptionValue (html, option, value, 0);
    
    // System.out.println (html.asText ());
    
    assertTrue (text.contains (html.asText ()));
  }
}
