package org.avis.subscription.ast.nodes;

import java.util.Map;

import org.avis.subscription.ast.NameParentNode;

public class Field extends NameParentNode
{
  public Field (String name)
  {
    super (name);
  }
  
  public String fieldName ()
  {
    return name;
  }
  
  @Override
  public String expr ()
  {
    return "field";
  }

  @Override
  public Class<?> evalType ()
  {
    return Object.class;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    return attrs.get (name);
  }
}
