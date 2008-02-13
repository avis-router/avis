package org.avis.subscription.ast.nodes;

import java.util.List;
import java.util.regex.Pattern;

import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.StringCompareNode;

import static org.avis.subscription.ast.Nodes.createConjunction;
import static org.avis.util.Wildcard.toPattern;

/**
 * NOTE: no real spec for wildcard exists. This implements ? and *
 * only, which is what is supported by Mantara elvind 4.4.0.
 * 
 * @author Matthew Phillips
 */
public class StrWildcard extends StringCompareNode
{
  private Pattern wildcard;

  /**
   * Create from a list of arguments.
   */
  public static Node create (List<Node> args)
  {
    return createConjunction (StrWildcard.class, Node.class, Const.class, args);
  }
  
  public StrWildcard (Node stringExpr, Const stringConst)
  {
    super (stringExpr, stringConst);
    
    this.wildcard = toPattern (string);
  }

  @Override
  public String expr ()
  {
    return "wildcard";
  }
  
  @Override
  protected boolean evaluate (String string1, String string2)
  {
    return wildcard.matcher (string1).matches ();
  }
}
