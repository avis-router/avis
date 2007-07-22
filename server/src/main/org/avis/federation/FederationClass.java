package org.avis.federation;

import org.avis.subscription.ast.Node;
import org.avis.subscription.parser.ParseException;

public class FederationClass
{
  public final Node<?> incomingFilter;
  public final Node<?> outgoingFilter;

  public FederationClass (String incomingFilterExpr, String outgoingFilterExpr)
    throws ParseException
  {
    this (parse (incomingFilterExpr), parse (outgoingFilterExpr));
  }

  public FederationClass (Node<?> incomingFilter, Node<?> outgoingFilter)
  {
    this.incomingFilter = incomingFilter;
    this.outgoingFilter = outgoingFilter;
  }

  private static Node<?> parse (String subExpr)
    throws ParseException
  {
    // todo
    return null;
  }
}
