package org.avis.pubsub.ast.nodes;

import org.avis.pubsub.ast.MathParentNode;
import org.avis.pubsub.ast.Node;

public class MathMod extends MathParentNode
{
  public MathMod (Node child1,
                  Node child2)
  {
    super (child1, child2);
  }
  
  @Override
  public String expr ()
  {
    return "%";
  }
  
  @Override
  protected int evaluateInt32 (int number1, int number2)
  {
    return number1 % number2;
  }

  @Override
  protected long evaluateInt64 (long number1, long number2)
  {
    return number1 % number2;
  }

  @Override
  protected double evaluateReal64 (double number1, double number2)
  {
    return number1 % number2;
  }
}