package org.avis.pubsub.ast.nodes;

import java.util.List;

import org.avis.pubsub.ast.Node;
import org.avis.pubsub.ast.Nodes;
import org.avis.pubsub.ast.StringCompareNode;

public class StrContains extends StringCompareNode
{
  /**
   * Create from a list of arguments.
   */
  public static Node<Boolean> create (List<Node<String>> args)
  {
    return Nodes.createNary (StrContains.class, Node.class, Const.class, args);
  }
  
  public StrContains (Node<String> stringExpr, Const<String> stringConst)
  {
    super (stringExpr, stringConst);
  }

  @Override
  public String expr ()
  {
    return "contains";
  }
  
  @Override
  protected boolean evaluate (String string1, String string2)
  {
    return string1.contains (string2);
  }
}