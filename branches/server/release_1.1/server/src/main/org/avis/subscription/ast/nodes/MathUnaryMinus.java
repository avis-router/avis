package org.avis.subscription.ast.nodes;

import java.util.Collection;
import java.util.Map;

import org.avis.subscription.ast.IllegalChildException;
import org.avis.subscription.ast.Node;

import static java.util.Collections.singleton;

public class MathUnaryMinus extends Node
{
  private Node child;

  public MathUnaryMinus (Node child)
    throws IllegalChildException
  {
    Class<?> childType = child.evalType ();
    
    if (childType != Object.class &&
        !Number.class.isAssignableFrom (childType))
    {
      throw new IllegalChildException
        ("Unary minus requires a numeric argument", this, child);
    }
    
    this.child = child;
  }
  
  @Override
  public String expr ()
  {
    return "-";
  }
  
  @Override
  public String presentation ()
  {
    return name ();
  }
  
  @Override
  public Class<?> evalType ()
  {
    return child.evalType ();
  }
  
  @Override
  public boolean hasChildren ()
  {
    return true;
  }
  
  @Override
  public Collection<Node> children ()
  {
    return singleton (child);
  }
  
  @Override
  public Node inlineConstants ()
  {
    child = child.inlineConstants ();
    
    Object result = evaluate (EMPTY_NOTIFICATION);
    
    if (result != null)
      return new Const (result);
    else
      return this;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Object result = child.evaluate (attrs);
    
    if (result instanceof Integer)
      return -((Integer)result);
    else if (result instanceof Long)
      return -((Long)result);
    else if (result instanceof Double)
      return -((Double)result);
    else
      return BOTTOM;
  }
}
