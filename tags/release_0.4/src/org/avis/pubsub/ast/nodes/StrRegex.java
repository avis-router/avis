package org.avis.pubsub.ast.nodes;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.avis.pubsub.ast.Node;
import org.avis.pubsub.ast.Nodes;
import org.avis.pubsub.ast.StringCompareNode;

public class StrRegex extends StringCompareNode
{
  private Pattern regex;

  /**
   * Create from a list of arguments.
   */
  public static Node<Boolean> create (List<Node<String>> args)
  {
    return Nodes.createNary (StrRegex.class, Node.class, Const.class, args);
  }
  
  public StrRegex (Node<String> stringExpr, Const<String> stringConst)
    throws PatternSyntaxException
  {
    super (stringExpr, stringConst);
    
    this.regex = Pattern.compile (string, Pattern.DOTALL);
  }

  @Override
  public String expr ()
  {
    return "regex";
  }
  
  @Override
  protected boolean evaluate (String string1, String string2)
  {
    return regex.matcher (string1).find ();
  }
}
