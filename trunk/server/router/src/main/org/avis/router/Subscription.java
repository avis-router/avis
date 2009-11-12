package org.avis.router;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import java.io.StringReader;

import org.avis.security.Keys;
import org.avis.subscription.ast.Node;
import org.avis.subscription.parser.ParseException;
import org.avis.subscription.parser.SubscriptionParser;

import static org.avis.security.DualKeyScheme.Subset.CONSUMER;
import static org.avis.subscription.ast.Node.TRUE;

/**
 * Represents a client's subscription.
 *  
 * @author Matthew Phillips
 */
public class Subscription
{
  private static final AtomicLong idCounter = new AtomicLong ();
  
  public long id;
  public String expr;
  public boolean acceptInsecure;
  public Keys keys;

  public int notificationCount;

  private Node ast;

  public Subscription (String expr, Keys keys, boolean acceptInsecure)
    throws ParseException
  {
    this.expr = expr;
    this.keys = keys;
    this.acceptInsecure = acceptInsecure;
    this.ast = parse (expr);
    this.id = idCounter.incrementAndGet ();
    this.notificationCount = 0;
    
    keys.hashPrivateKeysForRole (CONSUMER);
  }

  public void updateExpression (String subscriptionExpr)
    throws ParseException
  {
    ast = parse (subscriptionExpr);
    expr = subscriptionExpr;
  }
  
  public boolean matches (Map<String, Object> attributes)
  {
    return ast.evaluate (attributes) == TRUE;
  }
  
  private static Node parse (String expr)
    throws ParseException
  {
    return new SubscriptionParser (new StringReader (expr)).parseAndValidate ();
  }
}
