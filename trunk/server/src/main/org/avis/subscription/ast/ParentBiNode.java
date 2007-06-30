package org.avis.subscription.ast;

import java.util.ArrayList;
import java.util.Collection;

import org.avis.subscription.ast.nodes.Const;

/**
 * Utility base class for nodes that take only two children.
 * 
 * @author Matthew Phillips
 */
public abstract class ParentBiNode<E, E_CHILD>
  extends Node<E>
{
  protected Node<? extends E_CHILD> child1;
  protected Node<? extends E_CHILD> child2;

  public ParentBiNode ()
  {
    // zip
  }
  
  public ParentBiNode (Node<? extends E_CHILD> child1,
                       Node<? extends E_CHILD> child2)
  {
    init (child1, child2);
  }

  protected void init (Node<? extends E_CHILD> newChild1,
                       Node<? extends E_CHILD> newChild2)
  {
    checkChild (newChild1);
    this.child1 = newChild1;
    
    checkChild (newChild2);
    this.child2 = newChild2;
  }
  
  private void checkChild (Node<? extends E_CHILD> child)
  {
    String error = validateChild (child);

    if (error != null)
      throw new IllegalChildException (error, this, child);
  }
  
  protected abstract String validateChild (Node<? extends E_CHILD> child);
  
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
  public Collection<? extends Node<? extends E_CHILD>> children ()
  {
    ArrayList<Node<? extends E_CHILD>> children =
      new ArrayList<Node<? extends E_CHILD>> (2);
    
    children.add (child1);
    children.add (child2);
    
    return children;
  }
  
  @Override
  public Node<E> inlineConstants ()
  {
    child1 = child1.inlineConstants ();
    child2 = child2.inlineConstants ();
    
    E result = evaluate (EMPTY_NOTIFICATION);
    
    if (result != null)
      return new Const<E> (result);
    else
      return this;
  }
}
