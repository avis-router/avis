package org.avis.subscription.ast.nodes;

import java.util.Collection;
import java.util.Map;

import org.avis.subscription.ast.Node;

import static java.util.Collections.singleton;

public class StrFoldCase extends Node
{
  private Node stringExpr;
  
  public StrFoldCase (Node stringExpr)
  {
    this.stringExpr = stringExpr;
  }
  
  @Override
  public Class<?> evalType ()
  {
    return String.class;
  }

  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Object result = stringExpr.evaluate (attrs);
    
    return result instanceof String ? ((String)result).toLowerCase () : null;
  }

  @Override
  public String expr ()
  {
    return "fold-case";
  }
  
  @Override
  public String presentation ()
  {
    return name (); 
  }

  @Override
  public Node inlineConstants ()
  {
    Object result = evaluate (EMPTY_NOTIFICATION);
    
    return result == null ? this : new Const (result);
  }
  
  @Override
  public boolean hasChildren ()
  {
    return true;
  }
  
  @Override
  public Collection<Node> children ()
  {
    return singleton (stringExpr);
  }
}
