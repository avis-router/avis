package org.avis.subscription.parser;

import java.util.regex.PatternSyntaxException;

import org.avis.subscription.ast.IllegalChildException;
import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.nodes.Const;
import org.avis.util.InvalidFormatException;
import org.avis.util.Text;

public abstract class SubscriptionParserBase
{
  abstract Node doParse ()
    throws ParseException;
  
  /**
   * Parse an expression with syntax and basic type checking. Does not check
   * for boolean result type or const-ness. See {@link #parseAndValidate} if
   * you want a guaranteed correct subscription expression.
   */
  public Node parse ()
    throws ParseException
  {
    try
    {
      return doParse ();
    } catch (IllegalChildException ex)
    {
      throw new ParseException ("Illegal expression: " + ex.getMessage ());
    } catch (TokenMgrError ex)
    {
      throw new ParseException (ex.getMessage ());
    } catch (PatternSyntaxException ex)
    {
      // regex () had an invalid pattern
      throw new ParseException
        ("Invalid regex \"" + ex.getPattern () + "\": " + ex.getDescription ());
    }
  }
  
  /**
   * Execute {@link #parse()} and validate the result is a
   * type-correct, non-constant subscription expression.
   * 
   * @throws ParseException if parse () fails or if the resulting
   *           expression is not a valid subscription.
   * @throws ConstantExpressionException if the expression is contant.
   */
  @SuppressWarnings("unchecked")
  public Node<Boolean> parseAndValidate ()
    throws ParseException, ConstantExpressionException
  {
    Node node = parse ();
    
    if (node.evalType () == Boolean.class)
    {
      node = node.inlineConstants ();
        
      if (!(node instanceof Const))
        return node;
      else
        throw new ConstantExpressionException ("Expression is " + node.expr ());
    } else
    {
      throw new ParseException
        ("Expression does not evaluate to boolean: " + node.expr ());
    }
  }
  
  /**
   * Strip backslashed codes from a string.
   */
  protected static String stripBackslashes (String text)
    throws ParseException
  {
    try
    {
      return Text.stripBackslashes (text);
    } catch (InvalidFormatException ex)
    {
      throw new ParseException ("Error in string: " + ex.getMessage ());
    }
  }

  protected static String className (Class type)
  {
    return Text.className (type);
  }
  
  /**
   * Generate a string describing the expected tokens from token info
   * in a parse exception. Cribbed from the ParseException.getMessage ()
   * method.
   */
  public static String expectedTokensFor (ParseException ex)
  {
    StringBuilder expected = new StringBuilder ();
    boolean first = true;
    
    for (int i = 0; i < ex.expectedTokenSequences.length; i++)
    {
      if (!first)
        expected.append (", ");
      
      first = false;
      
      for (int j = 0; j < ex.expectedTokenSequences [i].length; j++)
        expected.append (ex.tokenImage [ex.expectedTokenSequences [i] [j]]);
    }

    return expected.toString ();
  }
}