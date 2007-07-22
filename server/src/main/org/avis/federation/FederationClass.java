package org.avis.federation;

import java.io.StringReader;

import org.avis.subscription.ast.Node;
import org.avis.subscription.parser.ParseException;
import org.avis.subscription.parser.SubscriptionParser;

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
    return new SubscriptionParser 
      (new StringReader (subExpr)).parseAndValidate ();
  }
}
