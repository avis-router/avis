package org.avis.client;

import org.avis.io.messages.Nack;
import org.avis.io.messages.RequestMessage;
import org.avis.io.messages.SubAddRqst;
import org.avis.io.messages.SubModRqst;

/**
 * Thrown when a subscription parse error is detected by the router.
 * 
 * @author Matthew Phillips
 */
public class SubscriptionParseException extends RouterException
{
  /**
   * Rejection code indicating there was a syntax error that prevented
   * parsing. e.g. missing ")".
   */
  public static final int INVALID_SYNTAX = Nack.PARSE_ERROR;
  
  /**
   * Rejection code indicating the expression was constant. i.e it
   * matches everything or nothing. e.g. <tt>1 != 1</tt> or
   * <tt>string ('hello')</tt>.
   */
  public static final int TRIVIAL_EXPRESSION = Nack.EXP_IS_TRIVIAL;
  
  /**
   * The subscription expression that was rejected.
   */
  public final String expression;
  
  /**
   * The reason the expression was rejected: one of
   * {@link #INVALID_SYNTAX} or {@link #TRIVIAL_EXPRESSION}.
   */
  public final int reason;

  SubscriptionParseException (RequestMessage request, Nack nack)
  {
    super (nack);
    
    if (request instanceof SubAddRqst)
      expression = ((SubAddRqst)request).subscriptionExpr;
    else if (request instanceof SubModRqst)
      expression = ((SubModRqst)request).subscriptionExpr;
    else
      expression = null;
    
    reason = nack.error;
  }
}
