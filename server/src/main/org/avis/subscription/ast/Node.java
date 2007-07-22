package org.avis.subscription.ast;

import java.util.Collection;
import java.util.Map;

import org.avis.subscription.ast.nodes.Const;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import static org.avis.util.Text.className;

/**
 * The base class for all nodes in a subscription expression abstract
 * syntax tree.
 * 
 * @author Matthew Phillips
 */
public abstract class Node
{
  public static final Boolean TRUE = Boolean.TRUE;
  public static final Boolean FALSE = Boolean.FALSE;
  public static final Boolean BOTTOM = null;

  protected static final Map<String, Object> EMPTY_NOTIFICATION = emptyMap ();
  
  /**
   * Evaluate the expression tree rooted at this node and return the
   * result.
   * 
   * @param attrs The notification to match against.
   */
  public abstract Object evaluate (Map<String, Object> attrs);

  /**
   * The type of result that is guaranteed to be generated by {@link
   * #evaluate(Map)}.
   */
  public abstract Class<?> evalType ();

  /**
   * Recursively inline any constant child subexpressions of this node
   * (i.e. replace any child expressions that always evaluate to a
   * constant value with that value). If, as a result of inlining,
   * this node can itself be replaced by a new node (e.g. a
   * {@link Const} instance) the reference to the replacement node can
   * be returned instead "this".
   * 
   * @return Either a reference to the node or a new node that
   *         replaces this one.
   */
  public abstract Node inlineConstants ();
  
  /**
   * A debug-friendly string version of the node.
   */
  public abstract String presentation ();

  /**
   * A string version of the node suitable for use in generating a
   * formal tree expression.
   * 
   * @see Nodes#unparse(Node)
   */
  public abstract String expr ();

  /**
   * Returns {@link #name()}. Subclasses may override.
   */
  @Override
  public String toString ()
  {
    return name ();
  }

  /**
   * The human-readable name of the node. Uses the class name as default.
   */
  public String name ()
  {
    return className (getClass ());
  }
  
  /**
   * True if {@link #children()} would return a non-empty child node
   * collection.
   */
  public boolean hasChildren ()
  {
    return false;
  }
  
  /**
   * Get the children of the node (if any). The collection may not be
   * modified.
   * 
   * @see #hasChildren()
   */
  public Collection<? extends Node> children ()
  {
    return emptySet ();
  }
}
