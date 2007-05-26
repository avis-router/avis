package org.avis.subscription.ast.nodes;

import java.util.Map;

import org.avis.subscription.ast.Node;

public class Nan extends Node<Boolean>
{
  private String field;
  
  public Nan (Field field)
  {
    this (field.fieldName ());
  }

  public Nan (String field)
  {
    this.field = field;
  }

  @Override
  public Class evalType ()
  {
    return Boolean.class;
  }
  
  @Override
  public String presentation ()
  {
    return name ();
  }
  
  @Override
  public Node<Boolean> inlineConstants ()
  {
    return this;
  }

  @Override
  public Boolean evaluate (Map<String, Object> attrs)
  {
    Object value = attrs.get (field);
    
    if (!(value instanceof Double))
      return BOTTOM;
    else
      return ((Double)value).isNaN ();
  }

  @Override
  public String expr ()
  {
    return "nan '" + field + '\'';
  }
}
