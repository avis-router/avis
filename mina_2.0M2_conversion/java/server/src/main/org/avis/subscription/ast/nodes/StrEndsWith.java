package org.avis.subscription.ast.nodes;

import java.util.List;

import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.StringCompareNode;

import static org.avis.subscription.ast.Nodes.createConjunction;

public class StrEndsWith extends StringCompareNode
{
  /**
   * Create from a list of arguments.
   */
  public static Node create (List<Node> args)
  {
    return createConjunction (StrEndsWith.class, Node.class, Const.class, args);
  }
  
  public StrEndsWith (Node stringExpr, Const stringConst)
  {
    super (stringExpr, stringConst);
  }

  @Override
  public String expr ()
  {
    return "ends-with";
  }
  
  @Override
  protected boolean evaluate (String string1, String string2)
  {
    return string1.endsWith (string2);
  }
}
