package org.avis.pubsub.ast.nodes;

import java.util.List;

import org.avis.pubsub.ast.Node;
import org.avis.pubsub.ast.StringCompareNode;

import static org.avis.pubsub.ast.Nodes.createConjunction;

public class StrBeginsWith extends StringCompareNode
{
  /**
   * Create from a list of arguments.
   */
  public static Node create (List<Node> args)
  {
    return createConjunction (StrBeginsWith.class, Node.class, Const.class, args);
  }
  
  public StrBeginsWith (Node stringExpr, Const stringConst)
  {
    super (stringExpr, stringConst);
  }

  @Override
  public String expr ()
  {
    return "begins-with";
  }
  
  @Override
  protected boolean evaluate (String string1, String string2)
  {
    return string1.startsWith (string2);
  }
}
