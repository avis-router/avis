package org.avis.subscription.parser;

import java.io.StringReader;

import org.avis.subscription.ast.Node;

import org.junit.Test;

import static org.avis.subscription.ast.Nodes.unparse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test the subscription expression parser and validator.
 * 
 * @author Matthew Phillips
 */
public class JUTestParser
{
  /**
   * Test identifiers as per Appendix A of client spec.
   */
  @Test
  public void identifiers ()
    throws Exception
  {
    assertParsesTo ("hello", "(field 'hello')");
    assertParsesTo ("_hello", "(field '_hello')");
    assertParsesTo ("_", "(field '_')");
    assertParsesTo ("_1", "(field '_1')");
    
    for (int i = 0x21; i <= 0xff; i++)
    {
      if (i == 0x22 || i == 0x27 || i == 0x29 || i == 0x30 || i == 0x2c ||
          i == 0x5b || i == 0x5c || i == 0x5d)
        continue;
      
      assertParsesTo ("tricky" + (char)i + "id",
                      "(field 'tricky" + (char)i + "id')");
    }
  }
  
  @Test
  public void strings ()
    throws Exception
  {
    assertParsesTo ("'a\\n'", "(string 'an')");
    assertParsesTo ("'a\\\\'", "(string 'a\\')");
    assertParsesTo ("\"a\\\\!\"", "(string 'a\\!')");
    assertParsesTo ("\"a\\\"\"", "(string 'a\"')");
    
    assertParseError ("'a\\'");
  }
  
  @Test
  public void numbers ()
    throws Exception
  {
    assertParsesTo ("0.0", "(real64 0.0)");
    assertParsesTo ("3.4", "(real64 3.4)");
    assertParsesTo ("1.2e10", "(real64 1.2E10)");
    assertParsesTo ("1.2E10", "(real64 1.2E10)");
    assertParsesTo ("1.2E+10", "(real64 1.2E10)");
    assertParsesTo ("1.2E-10", "(real64 1.2E-10)");
    
    assertParseError ("0.");
    assertParseError (".1");
    assertParseError ("0.1e");
    assertParseError ("0.1e-");
    assertParseError ("0.1eg");
    assertParseError ("0.1f");
  }
  
  /**
   * Test parser doesn't get confused by a function names as a field.
   */
  @Test
  public void functionsAsFields ()
    throws Exception
  {
    assertParsesTo ("begins-with == 'hello'",
                    "(== (field 'begins-with') (string 'hello'))");
    assertParsesTo ("int32 == 32",
                    "(== (field 'int32') (int32 32))");
  }
  
  /**
   * Basic parse tests using comparison and logical comparators.
   */
  @Test
  public void basic () 
    throws Exception
  {
    assertParsesTo ("field1 > 2 && field2 == 'hello\\'there'",
                    "(&& (> (field 'field1') (int32 2)) (== (field 'field2') " +
                      "(string 'hello'there')))");
    
    assertParsesTo ("(field1 != 10L || field2 < 3.2) ^^ (field3 == \"hello\")",
                    "(^^ (|| (! (== (field 'field1') (int64 10))) " +
                      "(< (field 'field2') (real64 3.2))) " +
                      "(== (field 'field3') (string 'hello')))");
  }
  
  /**
   * Test the various functions.
   */
  @Test
  public void functions () 
    throws Exception
  {
    assertParsesTo ("size (name)", "(size 'name')");
    assertParsesTo ("require (name)", "(require 'name')");
    assertParsesTo ("int32 (name)", "(int32 name)");
    assertParsesTo ("begins-with (name, 'hello')", "(begins-with (field 'name') (string 'hello'))");
    assertParsesTo ("ends-with (name, 'hello')", "(ends-with (field 'name') (string 'hello'))");
    assertParsesTo ("contains (name, 'hello')", "(contains (field 'name') (string 'hello'))");
    assertParsesTo ("regex (name, 'hello!+')", "(regex (field 'name') (string 'hello!+'))");
    assertParsesTo ("wildcard (name, 'hel*lo?')", "(wildcard (field 'name') (string 'hel*lo?'))");
    assertParsesTo ("fold-case (name)", "(fold-case (field 'name'))");
    assertParsesTo ("decompose (name)", "(decompose (field 'name'))");
    assertParsesTo ("decompose-compat (name)", "(decompose-compat (field 'name'))");
    assertParsesTo ("equals (name, 'hello')", "(== (field 'name') (string 'hello'))");
    assertParsesTo ("equals ('hello', 'hello')", "(== (string 'hello') (string 'hello'))");
    assertParsesTo ("equals (name, 'hello', 'there')",
                    "(|| (== (field 'name') (string 'hello')) (== (field 'name') (string 'there')))");
    assertParsesTo ("equals (name, 'hello', 42)",
                    "(|| (== (field 'name') (string 'hello')) (== (field 'name') (int32 42)))");
    
    assertParseError ("equals ('hello', 'there', 1)");
    assertParseError ("begins-with (name, 1)");
    assertParseError ("ends-with (name, 1)");
    assertParseError ("contains (name, 1)");
    assertParseError ("fold-case (1)");
    assertParseError ("decompose (1)");
    assertParseError ("decompose-compat (1)");
    assertParseError ("regex (name, 1)");
    assertParseError ("regex (name, '(abc')");
    assertParseError ("foobar (name, 'hello')");
  }
  
  /**
   * Test infix/prefix math ops.
   */
  @Test
  public void mathOps ()
    throws Exception
  {
    assertParsesTo ("1 + 2", "(+ (int32 1) (int32 2))");
    assertParsesTo ("1 + 2 + 3", "(+ (+ (int32 1) (int32 2)) (int32 3))");
    assertParsesTo ("1 - 2", "(- (int32 1) (int32 2))");
    assertParsesTo ("1 - 2 - 3", "(- (- (int32 1) (int32 2)) (int32 3))");
    assertParsesTo ("1 - 2 + 3", "(- (int32 1) (+ (int32 2) (int32 3)))");
    
    assertParsesTo ("1 * 2 / 3", "(* (int32 1) (/ (int32 2) (int32 3)))");

    assertParsesTo ("1 % 2", "(% (int32 1) (int32 2))");
    
    // check "-" operator generates 0 - n
    assertParsesTo ("-2", "(- (int32 0) (int32 2))");

    assertParsesTo ("~10", "(~ (int32 10))");

    assertParsesTo ("1 & 2", "(& (int32 1) (int32 2))");
    assertParsesTo ("1 ^ 2", "(^ (int32 1) (int32 2))");
    assertParsesTo ("1 | 2", "(| (int32 1) (int32 2))");

    assertParsesTo ("1 << 2", "(<< (int32 1) (int32 2))");
    assertParsesTo ("1 >> 2", "(>> (int32 1) (int32 2))");
    assertParsesTo ("1 >>> 2", "(>>> (int32 1) (int32 2))");

    assertParsesTo ("(name1 & 0x0F) >> name2",
                    "(>> (& (field 'name1') (int32 15)) (field 'name2'))");
    
    assertParsesTo ("(name1 << 2L) | name2 & 0xF0",
                    "(| (<< (field 'name1') (int64 2)) " +
                      "(& (field 'name2') (int32 240)))");
    
    // check math in equals ()
    assertParsesTo ("equals (name, 1 + 2)",
                    "(== (field 'name') (+ (int32 1) (int32 2)))");
    
    // errors
    assertParseError ("1 + 'hello'");
    assertParseError ("'hello' + 'hello'");
    assertParseError ("1.0 << 2");
    assertParseError ("1 << 2.0");
    assertParseError ("~'hello'");
  }
  
  /**
   * Test error basic parse error detection. Some error detection
   * testing is also done as part of other tests.
   */
  @Test
  public void errors ()
    throws Exception
  {
    // token errors
    assertParseError ("'");
    assertParseError ("\"");
    
    // syntax errors
    assertParseError ("");
    assertParseError ("   ");
    assertParseError ("(1 > 2");
    assertParseError ("(1 > 2) (2 > 1)");
    
    // type errors
    assertParseError ("5 > 'name'");
    assertParseError ("field > 3 || 'name'");
    assertParseError ("field > 3 || 42");
    assertParseError ("'hello' && 'there'");
    
    // non-boolean expressions
    assertParseValidateError ("2");
    assertParseValidateError ("'hello'");
    
    // constant expressions
    assertParseValidateError ("1 == 1");
    assertParseValidateError ("field > 5 || 1 == 1");
  }
  
  /**
   * Test some more complex expressions from Sticker.
   */
  @Test
  public void tickerClientSubs ()
    throws Exception
  {
    parseAndValidate
      ("(require (Message) && require (From) && " +
       "(fold-case (Group) == \"matthew@home\" || " +
       "fold-case (From) == \"matthew@home\")) || " +
       "(require (TICKERTEXT) && require (USER) && " +
       "fold-case (TICKERTAPE) == \"matthew@home\" || " +
       "fold-case (USER) == \"matthew@home\")");
    
    parseAndValidate
      ("Presence-Protocol < 2000 && string (Groups) && string (User) && " +
       "equals (Presence-Info, \'initial\', \'update\', \'xyz\') && " +
       "fold-case (User) != \"matthew@home\" && " +
       "(contains (fold-case (Groups), \"|dsto|\", \"|elvin|\", \"|ticker-dev|\") || " +
       "(equals (fold-case (User), \"foobar@dsto\", \"frodo@home\")))");
  }
  
  private static void assertParseError (String expr)
  {
    try
    {
      parse (expr);
      
      fail ("Parse error not detected: \"" + expr + "\"");
    } catch (ParseException ex)
    {
      // System.out.println ("parse error: " + ex);
      // ok
    }
  }
  
  private static void assertParseValidateError (String expr)
  {
    try
    {
      parseAndValidate (expr);
      
      fail ("Validate error not detected: \"" + expr + "\"");
    } catch (ParseException ex)
    {
      // System.out.println ("parse error: " + ex);
      // ok
    }
  }
  
  private static void assertParsesTo (String subExpr, String treeExpr)
    throws ParseException
  {
    assertEquals (treeExpr, unparse (parse (subExpr)));
  }
  
  private static Node parseAndValidate (String expr)
    throws ParseException
  {
    return new SubscriptionParser (new StringReader (expr)).parseAndValidate ();
  }
  
  private static Node parse (String expr)
    throws ParseException
  {
    return new SubscriptionParser (new StringReader (expr)).parse ();
  }
}
