package org.avis.subscription.ast.nodes;

import java.util.Map;

import org.avis.subscription.ast.NameParentNode;

/**
 * Test whether a field is a given type. Can be used for int32(),
 * int64(), string(), etc functions.
 * 
 * @author Matthew Phillips
 */
public class Type extends NameParentNode
{
  public Class<?> type;

  public Type (Field field, Class<?> type)
  {
    this (field.fieldName (), type);
  }

  public Type (String field, Class<?> type)
  {
    super (field);
    
    this.type = type;
  }

  @Override
  public Class<?> evalType ()
  {
    return Boolean.class;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Object value = attrs.get (name);
    
    if (value == null)
      return BOTTOM;
    else
      return type == value.getClass ();
  }

  @Override
  public String expr ()
  {
    if (type == Integer.class)
      return "int32";
    else if (type == Long.class)
      return "int64";
    else if (type == Double.class)
      return "real64";
    else if (type == String.class)
      return "string";
    else if (type == byte [].class)
      return "opaque";
    else
      return type.getClass ().getName ();
  }
}
