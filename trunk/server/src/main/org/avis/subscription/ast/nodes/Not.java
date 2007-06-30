package org.avis.subscription.ast.nodes;

import java.util.Collection;
import java.util.Map;

import org.avis.subscription.ast.Node;

import static java.util.Collections.singleton;

public class Not extends Node<Boolean>
{
  private Node<Boolean> child;
  
  public Not (Node<Boolean> child)
  {
    this.child = child;
  }

  @Override
  public String presentation ()
  {
    return "Not";
  }
  
  @Override
  public String expr ()
  {
    return "!";
  }
  
  @Override
  public Node<Boolean> inlineConstants ()
  {
    child = child.inlineConstants ();
    
    Boolean result = evaluate (EMPTY_NOTIFICATION);
    
    if (result != BOTTOM)
      return Const.bool (result);
    else
      return this;
  }
  
  @Override
  public Boolean evaluate (Map<String, Object> attrs)
  {
    Boolean result = child.evaluate (attrs);
    
    if (result == BOTTOM)
      return BOTTOM;
    else
      return !result;
  }
  
  @Override
  public boolean hasChildren ()
  {
    return true;
  }
  
  @Override
  public Collection<? extends Node<?>> children ()
  {
    return singleton (child);
  }
  
  @Override
  public Class<? extends Boolean> evalType ()
  {
    return Boolean.class;
  }
}
