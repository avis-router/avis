package org.avis.util;

import java.util.regex.Pattern;

import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.isWhitespace;
import static java.util.regex.Pattern.compile;

/**
 * Generates regex patterns that match Unix-style wildcards * and ?.
 * Currently handles * and ? by generating regex equivalents.
 */
public final class Wildcard
{
  private Wildcard ()
  {
    // zip
  }
  
  /**
   * Generate a patten matcher for a wildcard expression.
   * 
   * @param wildcard The wildcard expression.
   * 
   * @return A pattern that matches the wildcard.
   */
  public static Pattern toPattern (String wildcard)
  {
    return toPattern (wildcard, 0);
  }
  
  /**
   * Generate a patten matcher for a wildcard expression.
   * 
   * @param wildcard The wildcard expression.
   * @param flags The patten flags (e.g Pattern.CASE_INSENSITIVE).
   * 
   * @return A pattern that matches the wildcard.
   */
  public static Pattern toPattern (String wildcard, int flags)
  {
    StringBuilder regex = new StringBuilder (wildcard.length () * 2);
    boolean inRange = false;
    
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
        case '[':
          inRange  = true;
          regex.append ('[');
          break;
        case ']':
          inRange = false;
          regex.append (']');
          break;
        default:
          if (inRange || isLetterOrDigit (c) || isWhitespace (c))
            regex.append (c);
          else
            regex.append ('\\').append (c);
      }
    }
    
    return compile (regex.toString (), flags);
  }
}
