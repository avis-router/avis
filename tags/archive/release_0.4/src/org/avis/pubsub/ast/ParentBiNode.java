package org.avis.pubsub.ast;

import java.util.ArrayList;
import java.util.Collection;

import org.avis.pubsub.ast.nodes.Const;

/**
 * Base class for nodes that take only two children.
 * 
 * @author Matthew Phillips
 */
public abstract class ParentBiNode<EVAL_TYPE, CHILD_EVAL_TYPE>
  extends Node<EVAL_TYPE>
{
  protected Node<? extends CHILD_EVAL_TYPE> child1;
  protected Node<? extends CHILD_EVAL_TYPE> child2;

  public ParentBiNode ()
  {
    // zip
  }
  
  public ParentBiNode (Node<? extends CHILD_EVAL_TYPE> child1,
                       Node<? extends CHILD_EVAL_TYPE> child2)
  {
    init (child1, child2);
  }

  protected void init (Node<? extends CHILD_EVAL_TYPE> newChild1,
                       Node<? extends CHILD_EVAL_TYPE> newChild2)
  {
    checkChild (newChild1);
    this.child1 = newChild1;
    
    checkChild (newChild2);
    this.child2 = newChild2;
  }
  
  private void checkChild (Node<? extends CHILD_EVAL_TYPE> child)
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
  public Collection<? extends Node<? extends CHILD_EVAL_TYPE>> children ()
  {
    ArrayList<Node<? extends CHILD_EVAL_TYPE>> children =
      new ArrayList<Node<? extends CHILD_EVAL_TYPE>> (2);
    
    children.add (child1);
    children.add (child2);
    
    return children;
  }
  
  @Override
  public Node<EVAL_TYPE> inlineConstants ()
  {
    child1 = child1.inlineConstants ();
    child2 = child2.inlineConstants ();
    
    EVAL_TYPE result = evaluate (EMPTY_NOTIFICATION);
    
    if (result != null)
      return new Const<EVAL_TYPE> (result);
    else
      return this;
  }
}
