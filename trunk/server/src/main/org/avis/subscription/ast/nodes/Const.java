package org.avis.subscription.ast.nodes;

import java.util.Map;

import org.avis.subscription.ast.Node;

public class Const extends Node
{
  public static final Const CONST_FALSE = new Const (FALSE);
  public static final Const CONST_TRUE = new Const (TRUE);
  public static final Const CONST_BOTTOM = new Const (BOTTOM);

  public static final Const CONST_ZERO = new Const (0);
  
  private Object value;

  public static Const bool (Boolean value)
  {
    if (value == TRUE)
      return CONST_TRUE;
    else if (value == FALSE)
      return CONST_FALSE;
    else
      throw new IllegalArgumentException ("Invalid value: " + value);
  }
  
  public static Const string (String string)
  {
    return new Const(string);
  }
  
  public static Const int32 (int value)
  {
    if (value == 0)
      return CONST_ZERO;
    else
      return new Const (value);
  }
  
  public static Node int64 (long value)
  {
    return new Const (value);
  }
  
  public static Node real64 (double value)
  {
    return new Const (value);
  }
   
  public Const (Object value)
  {
    this.value = value;
  }
  
  public Object value ()
  {
    return value;
  }

  @Override
  public Class<?> evalType ()
  {
    if (value == BOTTOM)
      return Boolean.class;
    else
      return value.getClass ();
  }

  @Override
  public Node inlineConstants ()
  {
    return this;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    return value;
  }

  @Override
  public String expr ()
  {
    if (value instanceof String)
      return "string '" + value + '\'';
    else if (value instanceof Number)
      return numExpr ();
    else
      return "constant '" + value + '\'';
  }

  @Override
  public String presentation ()
  {
    if (value instanceof String)
      return "Str: \"" + value + '\"';
    else if (value instanceof Number)
      return numPresentation ();
    else
      return "Const: " + value;
  }

  private String numPresentation ()
  {
    StringBuilder str = new StringBuilder ("Num: ");
    
    str.append (value);
    
    if (value instanceof Long)
      str.append ('L');
    else if (value instanceof Double)
      str.append (" (double)");
      
    return str.toString ();
  }
  
  private String numExpr ()
  {
    StringBuilder str = new StringBuilder ();
    
    if (value instanceof Integer)
      str.append ("int32");
    else if (value instanceof Long)
      str.append ("int64");
    else
      str.append ("real64");
    
    str.append (' ').append (value);
    
    return str.toString ();
  }
}
