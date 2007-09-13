package org.avis.subscription.ast;

import static org.avis.util.Text.className;

/**
 * Base class for math nodes that require integer arguments.
 * 
 * @author Matthew Phillips
 */
public abstract class MathIntParentNode extends MathParentNode
{
  public MathIntParentNode (Node child1,
                            Node child2)
  {
    super (child1, child2);
  }
  
  @Override
  public Class<?> evalType ()
  {
    return Integer.class;
  }
  
  @Override
  protected String validateChild (Node child)
  {
    Class<?> childType = child.evalType ();
    
    if (childType == Object.class)
    {
      // allow generic nodes such as fields
      return null; 
    } else if (!(Integer.class.isAssignableFrom (childType) ||
                 Long.class.isAssignableFrom (childType)))
    {
      return "\"" + expr () + "\" needs an integer as an argument (was " +
             className (child.evalType ()).toLowerCase () + ")";
    } else 
    {
      return null;
    }
  }
  
  @Override
  protected boolean validOperand (Object value)
  {
    return value instanceof Integer || value instanceof Long;
  }
  
  @Override
  protected double evaluateReal64 (double number1, double number2)
  {
    throw new UnsupportedOperationException ("Not applicable for Real64 values");
  }
}
