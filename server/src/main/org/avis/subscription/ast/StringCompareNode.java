package org.avis.subscription.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.avis.subscription.ast.nodes.Const;

/**
 * Base class for nodes that compare a string expression with a string
 * argument.
 * 
 * @author Matthew Phillips
 */
public abstract class StringCompareNode extends Node<Boolean>
{
  protected Node<String> stringExpr;
  protected String string;

  public StringCompareNode (Node<String> stringExpr, Const<String> stringConst)
  {
    this (stringExpr, stringConst.value ());
  }
  
  public StringCompareNode (Node<String> stringExpr, String string)
  {
    this.stringExpr = stringExpr;
    this.string = string;
  }

  @Override
  public Class<Boolean> evalType ()
  {
    return Boolean.class;
  }

  @Override
  public String presentation ()
  {
    return name ();
  }
  
  @Override
  public boolean hasChildren ()
  {
    return true;
  }
  
  @Override
  public Collection<? extends Node<?>> children ()
  {
    ArrayList<Node<?>> children = new ArrayList<Node<?>> (2);
    
    children.add (stringExpr);
    children.add (new Const<String> (string));
    
    return children;
  }
  
  @Override
  public Boolean evaluate (Map<String, Object> attrs)
  {
    String value = stringExpr.evaluate (attrs);
    
    if (value == null)
      return BOTTOM;
    else
      return evaluate (value, string);
  }
  
  protected abstract boolean evaluate (String string1, String string2);
  
  @Override
  public Node<Boolean> inlineConstants ()
  {
    stringExpr = stringExpr.inlineConstants ();
    
    Boolean result = evaluate (EMPTY_NOTIFICATION);
    
    if (result != null)
      return Const.bool (result);
    else
      return this;
  }
}
