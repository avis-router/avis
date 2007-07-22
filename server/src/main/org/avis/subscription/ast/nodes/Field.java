package org.avis.subscription.ast.nodes;

import java.util.Map;

import org.avis.subscription.ast.Node;

public class Field extends Node
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
  public Class<?> evalType ()
  {
    return Object.class;
  }
  
  @Override
  public Node inlineConstants ()
  {
    return this;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    return attrs.get (name);
  }
}
