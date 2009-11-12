package org.avis.federation;

import java.util.Map;

import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.Nodes;
import org.avis.subscription.parser.ParseException;
import org.avis.subscription.parser.SubscriptionParserBase;

import static java.util.Collections.emptyMap;

import static org.avis.subscription.ast.nodes.Const.CONST_FALSE;
import static org.avis.subscription.ast.nodes.Const.CONST_TRUE;

/**
 * Defines a federation class which controls which notifications to
 * export and import from a router.
 * 
 * @see FederationClasses
 * 
 * @author Matthew Phillips
 */
public class FederationClass
{
  public String name;
  
  public Node incomingFilter;
  public Node outgoingFilter;
  
  public Map<String, Object> incomingAttributes;
  public Map<String, Object> outgoingAttributes;
  
  /**
   * Create a new instance with both filters set to false.
   */
  public FederationClass ()
  {
    this (CONST_FALSE, CONST_FALSE);
  }
  
  public FederationClass (String incomingFilterExpr, String outgoingFilterExpr)
    throws ParseException
  {
    this (parse (incomingFilterExpr), parse (outgoingFilterExpr));
  }

  public FederationClass (Node incomingFilter, Node outgoingFilter)
  {
    this.incomingFilter = incomingFilter;
    this.outgoingFilter = outgoingFilter;
    this.incomingAttributes = emptyMap ();
    this.outgoingAttributes = emptyMap ();
  }
  
  /**
   * True if this class neither exports nor imports anything.
   */
  public boolean allowsNothing ()
  {
    return incomingFilter == CONST_FALSE && outgoingFilter == CONST_FALSE;
  }

  /**
   * Parse a subscription expression, allowing TRUE and FALSE to stand for
   * CONST_TRUE and CONST_FALSE.
   * 
   * @see #unparse(Node)
   */
  public static Node parse (String subExpr)
    throws ParseException
  {
    subExpr = subExpr.trim ();
    
    if (subExpr.equalsIgnoreCase ("true"))
    {
      return CONST_TRUE;
    } else if (subExpr.equalsIgnoreCase ("false"))
    {
      return CONST_FALSE;
    } else
    {
      return SubscriptionParserBase.parse (subExpr); 
    }
  }
  
  /**
   * Unparse a subscription expression, allowing TRUE and FALSE to
   * stand for CONST_TRUE and CONST_FALSE.
   * 
   * @see #parse(String)
   */
  public static String unparse (Node node)
  {
    if (node == CONST_TRUE)
      return "TRUE";
    else if (node == CONST_FALSE)
      return "FALSE";
    else
      return Nodes.unparse (node); // TODO
  }
}
