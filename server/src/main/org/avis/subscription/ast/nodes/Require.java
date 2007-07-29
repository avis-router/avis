package org.avis.subscription.ast.nodes;

import java.util.Map;

import org.avis.subscription.ast.Node;

public class Require extends Node
{
  public String name;

  public Require (Field field)
  {
    this (field.fieldName ());
  }

  public Require (String name)
  {
    this.name = name;
  }
  
  @Override
  public String presentation ()
  {
    return name ();
  }

  @Override
  public Class<?> evalType ()
  {
    return Boolean.class;
  }

  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    return attrs.containsKey (name) ? TRUE : BOTTOM;
  }

  @Override
  public String expr ()
  {
    return "require '" + name + '\'';
  }

  @Override
  public Node inlineConstants ()
  {
    return this;
  }
}
