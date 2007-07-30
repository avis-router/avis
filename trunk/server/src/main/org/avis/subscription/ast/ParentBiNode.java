package org.avis.subscription.ast;

import java.util.ArrayList;
import java.util.Collection;

import org.avis.subscription.ast.nodes.Const;

/**
 * Utility base class for nodes that take only two children.
 * 
 * @author Matthew Phillips
 */
public abstract class ParentBiNode
  extends Node
{
  protected Node child1;
  protected Node child2;

  public ParentBiNode ()
  {
    // zip
  }
  
  public ParentBiNode (Node child1,
                       Node child2)
  {
    init (child1, child2);
  }

  protected void init (Node newChild1,
                       Node newChild2)
  {
    checkChild (newChild1);
    this.child1 = newChild1;
    
    checkChild (newChild2);
    this.child2 = newChild2;
  }
  
  private void checkChild (Node child)
  {
    String error = validateChild (child);

    if (error != null)
      throw new IllegalChildException (error, this, child);
  }
  
  protected abstract String validateChild (Node child);
  
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
    ArrayList<Node> children =
      new ArrayList<Node> (2);
    
    children.add (child1);
    children.add (child2);
    
    return children;
  }
  
  @Override
  public Node inlineConstants ()
  {
    child1 = child1.inlineConstants ();
    child2 = child2.inlineConstants ();
    
    Object result = evaluate (EMPTY_NOTIFICATION);
    
    if (result != null)
      return new Const (result);
    else
      return this;
  }
}
