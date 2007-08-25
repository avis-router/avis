package org.avis.federation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.avis.config.OptionTypeParam;
import org.avis.config.Options;
import org.avis.subscription.ast.Node;
import org.avis.subscription.parser.ParseException;
import org.avis.util.IllegalOptionException;
import org.avis.util.Pair;

import org.junit.Test;

import static org.avis.federation.FederationClass.parse;
import static org.avis.subscription.ast.Nodes.unparse;
import static org.avis.util.Collections.list;
import static org.avis.util.Collections.set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JUTestFederationOptions
{
  @SuppressWarnings("unchecked")
  @Test
  public void basic () 
    throws Exception
  {
    Options options = new Options (FederationOptionSet.OPTION_SET);
    
    Properties props = new Properties ();
    
    props.setProperty ("Federation.Activated", "Yes");
    props.setProperty ("Federation.Subscribe[Internal]", "TRUE");
    props.setProperty ("Federation.Subscribe[External]", "require (Message)");
    props.setProperty ("Federation.Provide[Internal]", "TRUE");
    props.setProperty ("Federation.Connect[Internal]", "ewaf://localhost");
    props.setProperty ("Federation.Connect[External]", 
                       "ewaf://public.elvin.org");
    props.setProperty ("Federation.Apply-Class[External]", 
                       "@.elvin.org domain");
    props.setProperty ("Federation.Listen", 
                       "ewaf://0.0.0.0 ewaf://hello:7778");
    props.setProperty ("Federation.Add-Incoming-Attribute[External][String]", 
                       "'hello'");
    props.setProperty ("Federation.Add-Incoming-Attribute[External][Int32]", 
                       "42");
    props.setProperty ("Federation.Add-Incoming-Attribute[External][Int64]",
                       "12L");
    props.setProperty ("Federation.Add-Incoming-Attribute[External][Real64]", 
                       "0.1");
    props.setProperty ("Federation.Add-Incoming-Attribute[External][Opaque]", 
                       "[de ad]");
    props.setProperty ("Federation.Add-Outgoing-Attribute[External][String]", 
                      "'hello world'");
    
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
    
    Map<String, Object> addIncomingAttribute = 
      options.getParamOption ("Federation.Add-Incoming-Attribute");
    
    Map<String, Object> incomingAttrs =
      (Map<String, Object>)addIncomingAttribute.get ("External");
    
    assertEquals ("hello", incomingAttrs.get ("String"));
    assertEquals (42, incomingAttrs.get ("Int32"));
    assertEquals (12L, incomingAttrs.get ("Int64"));
    assertEquals (0.1, incomingAttrs.get ("Real64"));
    assertTrue (Arrays.equals (new byte [] {(byte)0xde, (byte)0xad}, 
                               (byte [])incomingAttrs.get ("Opaque")));
    
    Map<String, Object> addOutgoingAttribute = 
      options.getParamOption ("Federation.Add-Outgoing-Attribute");
    
    Map<String, Object> outgoingAttrs =
      (Map<String, Object>)addOutgoingAttribute.get ("External");
    
    assertEquals ("hello world", outgoingAttrs.get ("String"));
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
      OptionTypeParam.splitOptionParam ("Base[Param1][Param2]");
    
    assertEquals ("Base", result.item1);
    assertEquals (list ("Param1", "Param2"), result.item2);
    
    result = OptionTypeParam.splitOptionParam ("Base[Param1]");
    
    assertEquals ("Base", result.item1);
    assertEquals (list ("Param1"), result.item2);
    
    result = OptionTypeParam.splitOptionParam ("Base");
    
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
      OptionTypeParam.splitOptionParam (expr);
      
      fail ();
    } catch (IllegalOptionException ex)
    {
      // ok
    }
  }
}
