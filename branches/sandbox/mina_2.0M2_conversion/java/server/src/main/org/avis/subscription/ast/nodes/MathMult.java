package org.avis.subscription.ast.nodes;

import org.avis.subscription.ast.MathParentNode;
import org.avis.subscription.ast.Node;

public class MathMult extends MathParentNode
{
  public MathMult (Node child1,
                   Node child2)
  {
    super (child1, child2);
  }
  
  @Override
  public String expr ()
  {
    return "*";
  }
  
  @Override
  protected int evaluateInt32 (int number1, int number2)
  {
    return number1 * number2;
  }

  @Override
  protected long evaluateInt64 (long number1, long number2)
  {
    return number1 * number2;
  }

  @Override
  protected double evaluateReal64 (double number1, double number2)
  {
    return number1 * number2;
  }
}
