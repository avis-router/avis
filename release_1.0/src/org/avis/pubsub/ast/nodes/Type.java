package org.avis.pubsub.ast.nodes;

import java.util.Map;

import org.avis.pubsub.ast.Node;

/**
 * Test whether a field is a given type. Can be used for int32(),
 * int64(), string(), etc functions.
 * 
 * @author Matthew Phillips
 */
public class Type extends Node<Boolean>
{
  private String field;
  private Class type;

  public Type (Field field, Class type)
  {
    this (field.fieldName (), type);
  }

  public Type (String field, Class type)
  {
    this.field = field;
    this.type = type;
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
    
    if (value == null)
      return BOTTOM;
    else
      return type == value.getClass ();
  }

  @Override
  public String expr ()
  {
    String op;
    
    if (type == Integer.class)
      op = "int32";
    else if (type == Long.class)
      op = "int64";
    else if (type == Double.class)
      op = "real64";
    else if (type == String.class)
      op = "string";
    else if (type == byte [].class)
      op = "opaque";
    else
      op = type.getClass ().getName ();
    
    return op + ' ' + field;
  }
}
