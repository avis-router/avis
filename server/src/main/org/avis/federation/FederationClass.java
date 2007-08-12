package org.avis.federation;

import org.avis.subscription.ast.Node;
import org.avis.subscription.parser.ParseException;
import org.avis.subscription.parser.SubscriptionParserBase;

import static org.avis.subscription.ast.nodes.Const.CONST_FALSE;
import static org.avis.subscription.ast.nodes.Const.CONST_TRUE;

public class FederationClass
{
  public Node incomingFilter;
  public Node outgoingFilter;

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
  }
  
  /**
   * True if this class neither exports nor imports anything.
   */
  public boolean isNull ()
  {
    return incomingFilter == CONST_FALSE && outgoingFilter == CONST_FALSE;
  }

  public static Node parse (String subExpr)
    throws ParseException
  {
    subExpr = subExpr.trim ();
    
    if (subExpr.equalsIgnoreCase ("true"))
    {
      // todo is this the correct way to subscribe to all? can you sub all?
      return CONST_TRUE;
    } else if (subExpr.equalsIgnoreCase ("false"))
    {
      return CONST_FALSE;
    } else
    {
      return SubscriptionParserBase.parse (subExpr); 
    }
  }
}
