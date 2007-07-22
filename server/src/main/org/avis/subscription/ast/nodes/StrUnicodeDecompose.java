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
@SuppressWarnings("restriction")
public class StrUnicodeDecompose extends Node
{
  public static final Mode DECOMPOSE = Normalizer.DECOMP;
  public static final Mode DECOMPOSE_COMPAT = Normalizer.DECOMP_COMPAT;
  
  private Node stringExpr;
  private Mode mode;

  public StrUnicodeDecompose (Node stringExpr, Mode mode)
  {
    this.stringExpr = stringExpr;
    this.mode = mode;
  }
  
  @Override
  public Class<?> evalType ()
  {
    return String.class;
  }

  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    String result = (String)stringExpr.evaluate (attrs);
    
    return result == null ? null : normalize (result, mode, 0);
  }

  @Override
  public String expr ()
  {
    return mode == DECOMPOSE ? "decompose" : "decompose-compat";
  }

  @Override
  public Node inlineConstants ()
  {
    Object result = evaluate (EMPTY_NOTIFICATION);
    
    return result == null ? this : new Const (result);
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
  public Collection<? extends Node> children ()
  {
    return singleton (stringExpr);
  }
}
