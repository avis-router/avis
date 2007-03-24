package org.avis.pubsub.ast.nodes;

import org.avis.pubsub.ast.MathIntParentNode;
import org.avis.pubsub.ast.Node;

public class MathBitShiftRight extends MathIntParentNode
{
  public MathBitShiftRight (Node<? extends Number> child1,
                            Node<? extends Number> child2)
  {
    super (child1, child2);
  }
  
  @Override
  public String expr ()
  {
    return ">>";
  }
  
  @Override
  protected int evaluateInt32 (int number1, int number2)
  {
    return number1 >> number2;
  }

  @Override
  protected long evaluateInt64 (long number1, long number2)
  {
    return number1 >> number2;
  }
}
