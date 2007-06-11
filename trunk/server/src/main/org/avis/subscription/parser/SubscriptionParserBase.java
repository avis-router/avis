package org.avis.subscription.parser;

import java.util.regex.PatternSyntaxException;

import org.avis.subscription.ast.IllegalChildException;
import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.nodes.Const;
import org.avis.util.Text;

public abstract class SubscriptionParserBase
{
  public abstract Node doParse ()
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
   * @throws ParseExpression if parse () fails or if the resulting
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
  {
    return Text.stripBackslashes (text);
  }
  
  /**
   * Expand backslash codes such as \n \x90 etc into their literal values.
   */
  protected static String expandBackslashes (String text)
  {
    if (text.indexOf ('\\') != -1)
    {
      StringBuilder buff = new StringBuilder (text.length ());
      
      for (int i = 0; i < text.length (); i++)
      {
        char c = text.charAt (i);
        
        if (c == '\\')
        {
          c = text.charAt (++i);
          
          switch (c)
          {
            case 'n':
              c = '\n'; break;
            case 't':
              c = '\t'; break;
            case 'b':
              c = '\b'; break;
            case 'r':
              c = '\r'; break;
            case 'f':
              c = '\f'; break;
            case 'a':
              c = 7; break;
            case 'v':
              c = 11; break;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
              int value = c - '0';
              int end = Math.min (text.length (), i + 3);
              
              while (i + 1 < end && octDigit (text.charAt (i + 1)))
              {
                c = text.charAt (++i);
                value = value * 8 + (c - '0');                
              }
              
              c = (char)value;
              break;
            case 'x':
              value = 0;
              end = Math.min (text.length (), i + 3);
              
              do
              {
                c = text.charAt (++i);
                value = value * 16 + hexValue (c);
              } while (i + 1 < end && hexDigit (text.charAt (i + 1)));
              
              c = (char)value;
              break;
          }
        }

        buff.append (c);
      }
      
      text = buff.toString ();
    }
    
    return text;
  }
  
  private static boolean octDigit (char c)
  {
    return c >= '0' && c <= '7';
  }
  
  private static boolean hexDigit (char c)
  {
    return (c >= '0' && c <= '9') ||
           (c >= 'a' && c <= 'f') ||
           (c >= 'A' && c <= 'F');
  }

  private static int hexValue (char c)
  {
    if (c >= '0' && c <= '9')
      return c - '0';
    else if (c >= 'a' && c <= 'f')
      return c - 'a' + 10;
    else
      return c - 'A' + 10;
  }

  protected static String className (Class type)
  {
    String name = type.getName ();
    
    return name.substring (name.lastIndexOf ('.') + 1);
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
