package org.avis.subscription.ast.nodes;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.StringCompareNode;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;

import static org.avis.subscription.ast.Nodes.createNary;

public class StrRegex extends StringCompareNode
{
  private Pattern regex;

  /**
   * Create from a list of arguments.
   */
  public static Node<Boolean> create (List<Node<String>> args)
  {
    return createNary (StrRegex.class, Node.class, Const.class, args);
  }
  
  public StrRegex (Node<String> stringExpr, Const<String> stringConst)
    throws PatternSyntaxException
  {
    super (stringExpr, stringConst);
    
    this.regex = compile (string, DOTALL);
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
