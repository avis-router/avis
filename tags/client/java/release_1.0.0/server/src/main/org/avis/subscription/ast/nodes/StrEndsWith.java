package org.avis.subscription.ast.nodes;

import java.util.List;

import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.StringCompareNode;

import static org.avis.subscription.ast.Nodes.createNary;

public class StrEndsWith extends StringCompareNode
{
  /**
   * Create from a list of arguments.
   */
  public static Node<Boolean> create (List<Node<String>> args)
  {
    return createNary (StrEndsWith.class, Node.class, Const.class, args);
  }
  
  public StrEndsWith (Node<String> stringExpr, Const<String> stringConst)
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
