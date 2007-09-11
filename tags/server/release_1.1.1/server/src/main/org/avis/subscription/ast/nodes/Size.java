package org.avis.subscription.ast.nodes;

import java.util.Map;

import org.avis.subscription.ast.NameParentNode;

public class Size extends NameParentNode
{
  public Size (Field field)
  {
    this (field.fieldName ());
  }

  public Size (String name)
  {
    super (name);
  }
  
  @Override
  public String expr ()
  {
    return "size";
  }
  
  @Override
  public Class<?> evalType ()
  {
    return Integer.class;
  }

  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Object value = attrs.get (name);
    
    if (value instanceof byte [])
      return ((byte [])value).length;
    else if (value instanceof String)
      return ((String)value).length ();
    else
      return BOTTOM;
  }
}
