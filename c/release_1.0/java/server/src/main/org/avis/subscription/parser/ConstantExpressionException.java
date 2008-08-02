package org.avis.subscription.parser;

/**
 * Thrown by parser when subscription expression is constant.
 */
public class ConstantExpressionException extends ParseException
{
  public ConstantExpressionException (String message)
  {
    super (message);
  }
}
