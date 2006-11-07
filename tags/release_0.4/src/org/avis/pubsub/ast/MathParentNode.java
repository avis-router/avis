package org.avis.pubsub.ast;

import java.util.Map;

import static org.avis.util.Numbers.highestPrecision;
import static org.avis.util.Numbers.upconvert;

/**
 * Base class for mathematical operator nodes. Subclasses implement
 * the evaluateXXX () methods to add math operation implementations.
 * 
 * @author Matthew Phillips
 */
public abstract class MathParentNode extends ParentBiNode<Number, Number>
{
  public MathParentNode (Node<? extends Number> child1,
                         Node<? extends Number> child2)
  {
    super (child1, child2);
  }

  @Override
  protected String validateChild (Node child)
  {
    Class childType = child.evalType ();
    
    if (childType == Object.class)
      return null; // allow generic nodes such as fields
    else if (!Number.class.isAssignableFrom (childType))
      return expr () + " needs a number as an argument (was " +
             Nodes.className (child.evalType ()) + ")";
    else
      return null;
  }
  
  @Override
  public Class evalType ()
  {
    return Number.class;
  }
 
  @Override
  public Number evaluate (Map<String, Object> attrs)
  {
    Object result1 = child1.evaluate (attrs);
    
    if (!validOperand (result1))
      return null;

    Object result2 = child2.evaluate (attrs);
    
    if (!validOperand (result2))
      return null;
    
    Class class1 = result1.getClass ();
    Class class2 = result2.getClass ();
    
    // check if upconvert needed
    if (class1 != class2)
    {
      Class newType = highestPrecision (class1, class2);
      
      if (class1 != newType)
        result1 = upconvert ((Number)result1, class2);
      else
        result2 = upconvert ((Number)result2, class1);
    }
    
    try
    {
      if (result1 instanceof Integer)
        return evaluateInt32 ((Integer)result1, (Integer)result2);
      else if (result1 instanceof Long)
        return evaluateInt64 ((Long)result1, (Long)result2);
      else
        return evaluateReal64 ((Double)result1, (Double)result2);
    } catch (ArithmeticException ex)
    {
      // e.g. div by zero. treat this is a bottom'ing condition
      return null;
    }
  }

  /**
   * Test whether value is a valid operand for this operation.
   */
  protected boolean validOperand (Object value)
  {
    return value instanceof Number;
  }

  protected abstract int evaluateInt32 (int number1, int number2);
  
  protected abstract long evaluateInt64 (long number1, long number2);
  
  protected abstract double evaluateReal64 (double number1, double number2);
}
