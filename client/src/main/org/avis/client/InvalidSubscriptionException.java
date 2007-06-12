package org.avis.client;

import org.avis.io.messages.Nack;
import org.avis.io.messages.RequestMessage;
import org.avis.io.messages.SubAddRqst;
import org.avis.io.messages.SubModRqst;

import static org.avis.io.messages.Nack.EXP_IS_TRIVIAL;

/**
 * Thrown when a subscription parse error is detected by the router.
 * 
 * @author Matthew Phillips
 */
public class InvalidSubscriptionException extends RouterNackException
{
  /**
   * Rejection code indicating there was a syntax error that prevented
   * parsing. e.g. missing ")".
   */
  public static final int SYNTAX_ERROR = 0;
  
  /**
   * Rejection code indicating the expression was constant. i.e it
   * matches everything or nothing. e.g. <tt>1 != 1</tt> or
   * <tt>string ('hello')</tt>.
   */
  public static final int TRIVIAL_EXPRESSION = 1;
  
  /**
   * The subscription expression that was rejected.
   */
  public final String expression;
  
  /**
   * The reason the expression was rejected: one of
   * {@link #SYNTAX_ERROR} or {@link #TRIVIAL_EXPRESSION}.
   */
  public final int reason;

  InvalidSubscriptionException (RequestMessage request, Nack nack)
  {
    super (request, nack);
    
    if (request instanceof SubAddRqst)
      expression = ((SubAddRqst)request).subscriptionExpr;
    else if (request instanceof SubModRqst)
      expression = ((SubModRqst)request).subscriptionExpr;
    else
      expression = null;
    
    reason = nack.error == EXP_IS_TRIVIAL ? TRIVIAL_EXPRESSION : SYNTAX_ERROR;
  }
}
