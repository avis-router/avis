package org.avis.subscription.ast;

import java.util.Collection;

import org.avis.subscription.ast.nodes.Field;

import static java.util.Collections.singleton;

/**
 * Base class for nodes that have a field name as their primary child
 * parameter and whose result is derived from that.
 * 
 * @author Matthew Phillips
 */
public abstract class NameParentNode extends Node
{
  public String name;
  
  public NameParentNode (Field field)
  {
    this (field.fieldName ());
  }
  
  public NameParentNode (String name)
  {
    this.name = name;
  }

  @Override
  public String presentation ()
  {
    return name ();
  }
  
  @Override
  public Node inlineConstants ()
  {
    // name-based nodes will not be inline-able
    return this;
  }

  @Override
  public boolean hasChildren ()
  {
    return true;
  }
  
  @Override
  public Collection<Node> children ()
  {
    return singleton ((Node)new Field (name));
  }
}
