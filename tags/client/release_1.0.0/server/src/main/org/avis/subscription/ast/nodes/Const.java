package org.avis.subscription.ast.nodes;

import java.util.Map;

import org.avis.subscription.ast.Node;

public class Const<E> extends Node<E>
{
  public static final Const<Boolean> CONST_FALSE = new Const<Boolean> (FALSE);
  public static final Const<Boolean> CONST_TRUE = new Const<Boolean> (TRUE);
  public static final Const<Boolean> CONST_BOTTOM = new Const<Boolean> (BOTTOM);

  public static final Const<Integer> CONST_ZERO = new Const<Integer> (0);
  
  private E value;

  public static Const<Boolean> bool (Boolean value)
  {
    if (value == TRUE)
      return CONST_TRUE;
    else if (value == FALSE)
      return CONST_FALSE;
    else
      throw new IllegalArgumentException ("Invalid value: " + value);
  }
  
  public static Const<String> string (String string)
  {
    return new Const<String> (string);
  }
  
  public static Const<Integer> int32 (int value)
  {
    if (value == 0)
      return CONST_ZERO;
    else
      return new Const<Integer> (value);
  }
  
  public static Node<Long> int64 (long value)
  {
    return new Const<Long> (value);
  }
  
  public static Node<Double> real64 (double value)
  {
    return new Const<Double> (value);
  }
   
  public Const (E value)
  {
    this.value = value;
  }
  
  public E value ()
  {
    return value;
  }

  @Override
  public Class evalType ()
  {
    if (value == BOTTOM)
      return Boolean.class;
    else
      return value.getClass ();
  }

  @Override
  public Node<E> inlineConstants ()
  {
    return this;
  }
  
  @Override
  public E evaluate (Map<String, Object> attrs)
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
