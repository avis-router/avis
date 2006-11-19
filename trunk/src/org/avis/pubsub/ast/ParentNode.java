package org.avis.pubsub.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Base class for nodes with an arbitrary number of children. This
 * class requires that all children have the same evaluation type.
 * 
 * @param EVAL_TYPE The type of value that this node evaluates to.
 * @param CHILD_TYPE The type that children of this node evaluate to.
 * 
 * @author Matthew Phillips
 */
public abstract class ParentNode<EVAL_TYPE, CHILD_TYPE> extends Node<EVAL_TYPE>
{
  protected ArrayList<Node<CHILD_TYPE>> children;
  
  public ParentNode ()
  {
    this.children = new ArrayList<Node<CHILD_TYPE>> (2);
  }

  public ParentNode (Node<? extends CHILD_TYPE> node1)
  {
    this ();
    
    addChild (node1);
  }
  
  public ParentNode (Node<? extends CHILD_TYPE> node1,
                     Node<? extends CHILD_TYPE> node2)
    throws IllegalChildException
  {
    this ();
    
    addChild (node1);
    addChild (node2);
  }

  /**
   * Note: this does not check child types.
   * 
   * @param children
   */
  public ParentNode (Node<CHILD_TYPE> ...children)
  {
    this.children =
      new ArrayList<Node<CHILD_TYPE>> (Arrays.asList (children));
  }

  /**
   * Called by {@link #addChild(Node)} to validate the child is valid
   * as a child.
   * 
   * @param child
   * 
   * @return An error message if not valid, null if child is OK to be
   *         added.
   */
  public abstract String validateChild (Node child);
  
  @SuppressWarnings("unchecked")
  public void addChild (Node child)
    throws IllegalChildException
  {
    String error = validateChild (child);
    
    if (error == null)
      children.add (child);
    else
      throw new IllegalChildException (error, this, child);
  }
  
  @Override
  public boolean hasChildren ()
  {
    return !children.isEmpty ();
  }
  
  @Override
  public Collection<? extends Node<?>> children ()
  {
    return children;
  }
}
