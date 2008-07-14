package org.avis.subscription.ast.nodes;

import java.util.Map;

import org.avis.subscription.ast.NameParentNode;

public class Nan extends NameParentNode
{
  public Nan (Field field)
  {
    this (field.fieldName ());
  }

  public Nan (String field)
  {
    super (field);
  }

  @Override
  public Class<?> evalType ()
  {
    return Boolean.class;
  }
  
  @Override
  public String expr ()
  {
    return "nan";
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Object value = attrs.get (name);
    
    if (!(value instanceof Double))
      return BOTTOM;
    else
      return ((Double)value).isNaN ();
  }
}
