package org.avis.subscription.ast;

public class IllegalChildException extends IllegalArgumentException
{
  public final Node parent;
  public final Node child;

  public IllegalChildException (Node parent, Node child)
  {
    this ("Illegal child: " + child.name (), parent, child);
  }

  public IllegalChildException (String message, Node parent, Node child)
  {
    super (message);
    
    this.parent = parent;
    this.child = child;
  }
}
