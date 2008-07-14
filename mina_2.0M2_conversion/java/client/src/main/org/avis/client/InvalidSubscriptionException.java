package org.avis.client;

import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.SubAddRqst;
import org.avis.io.messages.SubModRqst;

import static org.avis.io.messages.Nack.EXP_IS_TRIVIAL;
import static org.avis.io.messages.Nack.PARSE_ERROR;

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

  InvalidSubscriptionException (Message request, Nack nack)
  {
    super (textForErrorCode (nack.error) + ": " + nack.formattedMessage ());
    
    switch (request.typeId ())
    {
      case SubAddRqst.ID:
        expression = ((SubAddRqst)request).subscriptionExpr;
        break;
      case SubModRqst.ID:
        expression = ((SubModRqst)request).subscriptionExpr;
        break;
      default:
        expression = null;
    }

    reason = nack.error == EXP_IS_TRIVIAL ? TRIVIAL_EXPRESSION : SYNTAX_ERROR;
  }

  private static String textForErrorCode (int error)
  {
    switch (error)
    {
      case PARSE_ERROR:
        return "Syntax error";
      case EXP_IS_TRIVIAL:
        return "Trivial expression";
      default:
        return "Syntax error (" + error + ")";
    }
  }
}
