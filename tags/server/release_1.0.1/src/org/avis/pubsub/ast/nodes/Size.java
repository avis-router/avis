package org.avis.pubsub.ast.nodes;

import java.util.Map;

import org.avis.pubsub.ast.Node;

public class Size extends Node<Integer>
{
  private String name;

  public Size (Field<?> field)
  {
    this (field.fieldName ());
  }

  public Size (String name)
  {
    this.name = name;
  }
  
  @Override
  public String presentation ()
  {
    return name ();
  }

  @Override
  public String expr ()
  {
    return "size '" + name + '\'';
  }
  
  @Override
  public Class evalType ()
  {
    return Integer.class;
  }

  @Override
  public Integer evaluate (Map<String, Object> attrs)
  {
    Object value = attrs.get (name);
    
    if (value instanceof byte [])
      return ((byte [])value).length;
    else if (value instanceof String)
      return ((String)value).length ();
    else
      return null;
  }

  @Override
  public Node<Integer> inlineConstants ()
  {
    return this;
  }
}
