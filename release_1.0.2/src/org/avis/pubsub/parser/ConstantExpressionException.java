package org.avis.pubsub.parser;

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
