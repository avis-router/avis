package org.avis.subscription.ast.nodes;

import java.util.Collection;
import java.util.Map;

import org.avis.subscription.ast.Node;

import static java.util.Collections.singleton;

public class Not extends Node
{
  private Node child;
  
  public Not (Node child)
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
  public Node inlineConstants ()
  {
    child = child.inlineConstants ();
    
    Boolean result = (Boolean)evaluate (EMPTY_NOTIFICATION);
    
    if (result != BOTTOM)
      return Const.bool (result);
    else
      return this;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Boolean result = (Boolean)child.evaluate (attrs);
    
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
  public Collection<Node> children ()
  {
    return singleton (child);
  }
  
  @Override
  public Class<?> evalType ()
  {
    return Boolean.class;
  }
}
