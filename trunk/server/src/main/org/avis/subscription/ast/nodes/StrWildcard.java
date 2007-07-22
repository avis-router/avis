package org.avis.subscription.ast.nodes;

import java.util.List;
import java.util.regex.Pattern;

import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.StringCompareNode;

import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.isWhitespace;
import static java.util.regex.Pattern.compile;

import static org.avis.subscription.ast.Nodes.createConjunction;

/**
 * NOTE: no real spec for wildcard exists. This implements ? and *
 * only, which is what is supported by Mantara elvind 4.4.0.
 * 
 * @author Matthew Phillips
 */
public class StrWildcard extends StringCompareNode
{
  private Pattern regex;

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
    
    this.regex = compile (toRegex (string));
  }

  /**
   * Fairly dumb wildcard -> regex converter. Handles * and ? by
   * generating regex equivalents.
   */
  private static String toRegex (String wildcard)
  {
    StringBuilder regex = new StringBuilder (wildcard.length () * 2);
    
    for (int i = 0; i < wildcard.length (); i++)
    {
      char c = wildcard.charAt (i);
      
      switch (c)
      {
        case '*':
          regex.append (".*");
          break;
        case '?':
          regex.append ('.');
          break;
        case '\\':
          if (++i < wildcard.length ())
          {
            regex.append ('\\');
            regex.append (wildcard.charAt (i));
          } else
          {
            regex.append ("\\\\");
          }
          break;
        default:
          if (isLetterOrDigit (c) || isWhitespace (c))
            regex.append (c);
          else
            regex.append ('\\').append (c);
      }
    }
    
    return regex.toString ();
  }

  @Override
  public String expr ()
  {
    return "wildcard";
  }
  
  @Override
  protected boolean evaluate (String string1, String string2)
  {
    return regex.matcher (string1).matches ();
  }
}
