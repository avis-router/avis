package org.avis.pubsub.ast.nodes;

import java.util.Map;

import org.avis.pubsub.ast.Node;

public class Field<T> extends Node<T>
{
  public String name;
  
  public Field (String name)
  {
    this.name = name;
  }
  
  public String fieldName ()
  {
    return name;
  }
  
  @Override
  public String presentation ()
  {
    return "Field: \"" + name + '\'';
  }
  
  @Override
  public String expr ()
  {
    return "field '" + name + '\'';
  }

  @Override
  public Class evalType ()
  {
    return Object.class;
  }
  
  @Override
  public Node<T> inlineConstants ()
  {
    return this;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public T evaluate (Map<String, Object> attrs)
  {
    return (T)attrs.get (name);
  }
}
