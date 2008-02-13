package org.avis.client;

import java.io.StringReader;

import org.avis.subscription.ast.Node;
import org.avis.subscription.parser.ParseException;
import org.avis.subscription.parser.SubscriptionParser;

import org.junit.Test;

import static org.avis.client.Subscription.escapeField;
import static org.avis.client.Subscription.escapeString;
import static org.avis.subscription.ast.Nodes.unparse;

import static org.junit.Assert.assertEquals;

public class JUTestSubscriptionEscapes
{
  @Test
  public void escapes () 
    throws Exception
  {
    assertParsesAsField ("'hello'");
    assertParsesAsField ("hello there");
    assertParsesAsField ("123");
    assertParsesAsField ("1 + 2");
    assertParsesAsField ("foo (bar)");
    assertParsesAsField (" ");
    assertParsesAsField ("\\");

    assertParsesAsString ("123 'hello'");
    assertParsesAsString ("\"hello\"");
    assertParsesAsString ("\\");
  }

  private static void assertParsesAsString (String string)
    throws ParseException
  {
    assertParsesTo ('"' + escapeString (string) + '"', '\'' + string + '\'');
  }
  
  private static void assertParsesAsField (String fieldStr)
    throws ParseException
  {
    assertParsesTo (escapeField (fieldStr), "(field '" + fieldStr + "')");
  }
  
  private static void assertParsesTo (String subExpr, String treeExpr)
    throws ParseException
  {
    assertEquals (treeExpr, unparse (parse (subExpr)));
  }
  
  private static Node parse (String subExpr) 
    throws ParseException
  {
    return new SubscriptionParser (new StringReader (subExpr)).parse ();
  }
}
