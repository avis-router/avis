package org.avis.subscription.ast;

/**
 * Base class for parent nodes that evaludate to boolean from boolean
 * children.
 * 
 * @author Matthew Phillips
 */
public abstract class BoolParentNode
  extends ParentNode<Boolean, Boolean>
{
  public BoolParentNode ()
  {
    // zip
  }

  public BoolParentNode (Node<Boolean> node1)
  {
    super (node1);
  }

  public BoolParentNode (Node<Boolean> node1,
                         Node<Boolean> node2)
  {
    super (node1, node2);
  }

  public BoolParentNode (Node<Boolean>... children)
  {
    super (children);
  }
  
  @Override
  public String presentation ()
  {
    return name (); 
  }

  @Override
  public Class evalType ()
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
