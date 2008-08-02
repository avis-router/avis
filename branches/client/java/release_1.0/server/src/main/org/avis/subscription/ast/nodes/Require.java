package org.avis.subscription.ast.nodes;

import java.util.Map;

import org.avis.subscription.ast.NameParentNode;

public class Require extends NameParentNode
{
  public Require (Field field)
  {
    this (field.fieldName ());
  }

  public Require (String name)
  {
    super (name);
  }
  
  @Override
  public String expr ()
  {
    return "require";
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
}
