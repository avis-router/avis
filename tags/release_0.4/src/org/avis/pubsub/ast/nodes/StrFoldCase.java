package org.avis.pubsub.ast.nodes;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.avis.pubsub.ast.Node;

public class StrFoldCase extends Node<String>
{
  private Node<String> stringExpr;
  
  public StrFoldCase (Node<String> stringExpr)
  {
    this.stringExpr = stringExpr;
  }
  
  @Override
  public Class evalType ()
  {
    return String.class;
  }

  @Override
  public String evaluate (Map<String, Object> attrs)
  {
    String result = stringExpr.evaluate (attrs);
    
    return result == null ? null : result.toLowerCase ();
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
  public Node<String> inlineConstants ()
  {
    String result = evaluate (EMPTY_NOTIFICATION);
    
    return result == null ? this : new Const<String> (result);
  }
  
  @Override
  public boolean hasChildren ()
  {
    return true;
  }
  
  @Override
  public Collection<? extends Node<?>> children ()
  {
    return Collections.singleton (stringExpr);
  }
}
