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
public abstract class StringCompareNode extends Node
{
  protected Node stringExpr;
  protected String string;

  public StringCompareNode (Node stringExpr, Const stringConst)
  {
    this (stringExpr, (String)stringConst.value ());
  }
  
  public StringCompareNode (Node stringExpr, String string)
  {
    this.stringExpr = stringExpr;
    this.string = string;
  }

  @Override
  public Class<?> evalType ()
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
  public Collection<Node> children ()
  {
    ArrayList<Node> children = new ArrayList<Node> (2);
    
    children.add (stringExpr);
    children.add (new Const (string));
    
    return children;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Object exprValue = stringExpr.evaluate (attrs);
    
    if (exprValue instanceof String)
      return evaluate ((String)exprValue, string);
    else
      return BOTTOM;
  }
  
  protected abstract boolean evaluate (String string1, String string2);
  
  @Override
  public Node inlineConstants ()
  {
    stringExpr = stringExpr.inlineConstants ();
    
    Object result = evaluate (EMPTY_NOTIFICATION);
    
    if (result != BOTTOM)
      return Const.bool ((Boolean)result);
    else
      return this;
  }
}
