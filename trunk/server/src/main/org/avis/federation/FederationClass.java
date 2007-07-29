package org.avis.federation;

import org.avis.subscription.ast.Node;
import org.avis.subscription.parser.ParseException;
import org.avis.subscription.parser.SubscriptionParserBase;

import static org.avis.subscription.ast.nodes.Const.CONST_FALSE;
import static org.avis.subscription.ast.nodes.Const.CONST_TRUE;

public class FederationClass
{
  public final Node incomingFilter;
  public final Node outgoingFilter;

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

  private static Node parse (String subExpr)
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
