package org.avis.subscription.ast;

import java.util.Collection;

/**
 * Base class for parent nodes that evaluate to boolean from boolean
 * children.
 * 
 * @author Matthew Phillips
 */
public abstract class BoolParentNode
  extends ParentNode
{
  public BoolParentNode ()
  {
    // zip
  }

  public BoolParentNode (Node node1)
  {
    super (node1);
  }

  public BoolParentNode (Node node1,
                         Node node2)
  {
    super (node1, node2);
  }

  public BoolParentNode (Node... children)
  {
    super (children);
  }
  
  public BoolParentNode (Collection<? extends Node> children)
  {
    super (children);
  }

  @Override
  public String presentation ()
  {
    return name (); 
  }

  @Override
  public Class<?> evalType ()
  {
    return Boolean.class;
  }
  
  @Override
  public String validateChild (Node child)
  {
    if (child.evalType () != Boolean.class)
      return expr () + " requires boolean arguments (" + child.expr () + ")";
    else
      return null;
  }
}
