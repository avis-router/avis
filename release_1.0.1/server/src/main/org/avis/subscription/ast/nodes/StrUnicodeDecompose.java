package org.avis.subscription.ast.nodes;

import java.util.Collection;
import java.util.Map;

import org.avis.subscription.ast.Node;

import sun.text.Normalizer;
import sun.text.Normalizer.Mode;

import static java.util.Collections.singleton;
import static sun.text.Normalizer.normalize;

/**
 * TODO remove use of non-portable sun.text.Normalizer. Java 6 has new
 * java.text.Normalizer class.
 * 
 * @author Matthew Phillips
 */
public class StrUnicodeDecompose extends Node<String>
{
  public static final Mode DECOMPOSE = Normalizer.DECOMP;
  public static final Mode DECOMPOSE_COMPAT = Normalizer.DECOMP_COMPAT;
  
  private Node<String> stringExpr;
  private Mode mode;

  public StrUnicodeDecompose (Node<String> stringExpr, Mode mode)
  {
    this.stringExpr = stringExpr;
    this.mode = mode;
  }
  
  @Override
  public Class evalType ()
  {
    return String.class;
  }

  @Override
  public String evaluate (Map<String, Object> attrs)
  {
    String result = stringExpr.evaluate (attrs);
    
    return result == null ? null : normalize (result, mode, 0);
  }

  @Override
  public String expr ()
  {
    return mode == DECOMPOSE ? "decompose" : "decompose-compat";
  }

  @Override
  public Node<String> inlineConstants ()
  {
    String result = evaluate (EMPTY_NOTIFICATION);
    
    return result == null ? this : new Const<String> (result);
  }

  @Override
  public String presentation ()
  {
    return name ();
  }
  
  @Override
  public boolean hasChildren ()
  {
    return true;
  }
  
  @Override
  public Collection<? extends Node<?>> children ()
  {
    return singleton (stringExpr);
  }
}
