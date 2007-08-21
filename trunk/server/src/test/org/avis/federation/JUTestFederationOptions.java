package org.avis.federation;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.avis.subscription.ast.Node;
import org.avis.subscription.parser.ParseException;
import org.avis.util.IllegalOptionException;
import org.avis.util.Pair;

import static org.avis.federation.FederationClass.parse;
import static org.avis.federation.FederationOptions.splitOptionParam;
import static org.avis.subscription.ast.Nodes.unparse;
import static org.avis.util.Collections.list;
import static org.avis.util.Collections.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JUTestFederationOptions
{
  @Test
  public void basic () 
    throws Exception
  {
    FederationOptions options = new FederationOptions ();
    
    Properties props = new Properties ();
    
    props.setProperty ("Federation.Activated", "Yes");
    props.setProperty ("Federation.Subscribe[Internal]", "TRUE");
    props.setProperty ("Federation.Subscribe[External]", "require (Message)");
    props.setProperty ("Federation.Provide[Internal]", "TRUE");
    props.setProperty ("Federation.Connect[Internal]", "ewaf://localhost");
    props.setProperty ("Federation.Connect[External]", "ewaf://public.elvin.org");
    props.setProperty ("Federation.Apply-Class[External]", "@.elvin.org domain");
    props.setProperty ("Federation.Listen", "ewaf://0.0.0.0 ewaf://hello:7778");
    
    options.setAll (props);
    
    assertEquals (true, options.getBoolean ("Federation.Activated"));
    assertEquals (set (new EwafURI ("ewaf://0.0.0.0"), 
                       new EwafURI ("ewaf://hello:7778")), 
                  options.get ("Federation.Listen"));
    
    Map<String, Object> subscribe = 
      options.getParamOption ("Federation.Subscribe");
    
    assertEquals (astExpr ("require (Message)"), 
                  unparse ((Node)subscribe.get ("External")));
    
    assertEquals (astExpr ("TRUE"), 
                  unparse ((Node)subscribe.get ("Internal")));
    
    Map<String, Object> connect = 
      options.getParamOption ("Federation.Connect");
    
    assertEquals (new EwafURI ("ewaf://localhost"), 
                  connect.get ("Internal"));
    
    assertEquals (new EwafURI ("ewaf://public.elvin.org"), 
                  connect.get ("External"));
    
    Map<String, Object> applyClass = 
      options.getParamOption ("Federation.Apply-Class");
    
    assertEquals (set ("@.elvin.org", "domain"),  applyClass.get ("External"));
    
    assertEquals (new EwafURI ("ewaf://public.elvin.org"), 
                  connect.get ("External"));
  }

  private static String astExpr (String expr) 
    throws ParseException
  {
    return unparse (parse (expr));
  }
  
  @Test
  public void splitParams () 
    throws Exception
  {
    Pair<String,List<String>> result = 
      splitOptionParam ("Base[Param1][Param2]");
    
    assertEquals ("Base", result.item1);
    assertEquals (list ("Param1", "Param2"), result.item2);
    
    result = splitOptionParam ("Base[Param1]");
    
    assertEquals ("Base", result.item1);
    assertEquals (list ("Param1"), result.item2);
    
    result = splitOptionParam ("Base");
    
    assertEquals ("Base", result.item1);
    assertEquals (0, result.item2.size ());
    
    assertInvalidParam ("Base[");
    // todo
//    assertInvalidParam ("Base[[hello]");
    assertInvalidParam ("Base[hello");
    assertInvalidParam ("Base[hello[");
    assertInvalidParam ("Base[hello][");
    assertInvalidParam ("Base[hello]]");
    assertInvalidParam ("Base]");
  }

  private static void assertInvalidParam (String expr)
  {
    try
    {
      splitOptionParam (expr);
      
      fail ();
    } catch (IllegalOptionException ex)
    {
      // ok
    }
  }
}
