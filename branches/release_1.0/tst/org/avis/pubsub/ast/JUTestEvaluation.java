package org.avis.pubsub.ast;

import java.util.Arrays;
import java.util.List;

import java.io.StringReader;

import java.lang.reflect.Constructor;

import org.junit.Test;

import org.avis.common.Notification;
import org.avis.pubsub.ast.nodes.And;
import org.avis.pubsub.ast.nodes.Compare;
import org.avis.pubsub.ast.nodes.Const;
import org.avis.pubsub.ast.nodes.Field;
import org.avis.pubsub.ast.nodes.MathBitAnd;
import org.avis.pubsub.ast.nodes.MathBitInvert;
import org.avis.pubsub.ast.nodes.MathBitLogShiftRight;
import org.avis.pubsub.ast.nodes.MathBitOr;
import org.avis.pubsub.ast.nodes.MathBitShiftLeft;
import org.avis.pubsub.ast.nodes.MathBitShiftRight;
import org.avis.pubsub.ast.nodes.MathBitXor;
import org.avis.pubsub.ast.nodes.MathDiv;
import org.avis.pubsub.ast.nodes.MathMinus;
import org.avis.pubsub.ast.nodes.MathMod;
import org.avis.pubsub.ast.nodes.MathMult;
import org.avis.pubsub.ast.nodes.MathPlus;
import org.avis.pubsub.ast.nodes.Nan;
import org.avis.pubsub.ast.nodes.Not;
import org.avis.pubsub.ast.nodes.Or;
import org.avis.pubsub.ast.nodes.Require;
import org.avis.pubsub.ast.nodes.Size;
import org.avis.pubsub.ast.nodes.StrBeginsWith;
import org.avis.pubsub.ast.nodes.StrContains;
import org.avis.pubsub.ast.nodes.StrEndsWith;
import org.avis.pubsub.ast.nodes.StrFoldCase;
import org.avis.pubsub.ast.nodes.StrRegex;
import org.avis.pubsub.ast.nodes.StrUnicodeDecompose;
import org.avis.pubsub.ast.nodes.StrWildcard;
import org.avis.pubsub.ast.nodes.Type;
import org.avis.pubsub.ast.nodes.Xor;
import org.avis.pubsub.parser.ParseException;
import org.avis.pubsub.parser.SubscriptionParser;

import static java.lang.Double.NaN;
import static org.avis.pubsub.ast.Node.BOTTOM;
import static org.avis.pubsub.ast.Node.EMPTY_NOTIFICATION;
import static org.avis.pubsub.ast.Node.FALSE;
import static org.avis.pubsub.ast.Node.TRUE;
import static org.avis.pubsub.ast.nodes.StrUnicodeDecompose.DECOMPOSE;
import static org.avis.pubsub.ast.nodes.StrUnicodeDecompose.DECOMPOSE_COMPAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the evaluation of AST's.
 * 
 * @author Matthew Phillips
 */
public class JUTestEvaluation
{
  /** All tri-state logic states. */
  private static final Boolean [] LOGIC_STATES =
    new Boolean [] {TRUE, BOTTOM, FALSE};
  
  private static final Boolean [] NOT_TRUTH_TABLE =
    new Boolean [] {FALSE, BOTTOM, TRUE};
  
  private static final Boolean [] OR_TRUTH_TABLE =
    new Boolean [] {TRUE, TRUE, TRUE, TRUE, BOTTOM, BOTTOM, TRUE, BOTTOM, FALSE};
  
  private static final Boolean [] AND_TRUTH_TABLE =
    new Boolean [] {TRUE, BOTTOM, FALSE, BOTTOM, BOTTOM, FALSE, FALSE, FALSE, FALSE};

  private static final Boolean [] XOR_TRUTH_TABLE =
    new Boolean [] {FALSE, BOTTOM, TRUE, BOTTOM, BOTTOM, BOTTOM, TRUE, BOTTOM, FALSE};
  
  /**
   * Test NOT, AND, OR and XOR logic operators against their tri-state
   * truth tables.
   */
  @Test
  public void logicOps ()
    throws Exception
  {
    int index;
    
    // NOT
    index = 0;
    for (Boolean state : LOGIC_STATES)
    {
      assertEquals (NOT_TRUTH_TABLE [index],
                    new Not (new Const<Boolean> (state)).evaluate (EMPTY_NOTIFICATION));
      
      index++;
    }
    
    // OR, AND, XOR
    index = 0;
    for (Boolean a : LOGIC_STATES)
    {
      for (Boolean b : LOGIC_STATES)
      {
        checkTruthTable (And.class, a, b, AND_TRUTH_TABLE [index]);
        checkTruthTable (Or.class, a, b, OR_TRUTH_TABLE [index]);
        checkTruthTable (Xor.class, a, b, XOR_TRUTH_TABLE [index]);
      
        index++;
      }
    }
  }

  /**
   * Basic test for the Compare operator minus numeric conversion.
   */
  @Test
  public void compare ()
  {
    // less than: 10 < 20
    assertTrue ("10 < 20", compare (10, 20, -1, false));
    assertFalse ("10 < 10", compare (10, 10, -1, false));
        
    // less than or equal
    assertTrue ("10 <= 20", compare (10, 20, -1, true));
    assertTrue ("10 <= 10", compare (10, 10, -1, true));
    assertFalse ("20 <= 10", compare (20, 10, -1, true));
    
    // greater than
    assertTrue ("20 > 10", compare (20, 10, 1, false));
    assertFalse ("10 > 10", compare (10, 10, 1, false));
    
    //  greater than or equal
    assertTrue ("20 >= 10", compare (20, 10, 1, true));
    assertTrue ("10 >= 10", compare (10, 10, 1, true));
    assertFalse ("10 > 20", compare (10, 20, 1, true));
    
    // equal
    assertTrue ("10 == 10", compare (10, 10, 0, true));
    assertFalse ("10 == 20", compare (10, 20, 0, true));
  }

  /**
   * Test logic operator expressions using operators in LOGIC_OP_EXPR1
   * and LOGIC_OP_EXPR2.
   */
  @Test
  public void logicOpExprs ()
  {
    Node<Boolean> expr1 = logicOpExpr1 ();
    
    Notification n1 = new Notification ();
    n1.put ("name", "Matt");
    n1.put ("age", 19);
    n1.put ("blah", "blah");
    
    assertTrue (expr1.evaluate (n1));
    
    Notification n2 = new Notification ();
    n2.put ("name", "Matt");
    n2.put ("age", 30);
    n2.put ("blah", "blah");
    
    assertFalse (expr1.evaluate (n2));
    
    Notification n3 = new Notification ();
    n3.put ("name", "Matt");
    n3.put ("blah", "blah");
    
    assertEquals (BOTTOM, expr1.evaluate (n3));
    
    Notification n4 = new Notification ();
    n4.put ("name", "Matt");
    n4.put ("age", 19);
    n4.put ("blah", "frob");
    
    assertFalse (expr1.evaluate (n4));
    
    // XOR tests
    
    Node<Boolean> expr2 = logicOpExpr2 ();
    
    Notification n5 = new Notification ();
    n5.put ("name", "Matt");
    n5.put ("age", 5);
    
    assertTrue (expr2.evaluate (n5));
    
    Notification n6 = new Notification ();
    n6.put ("name", "Matt");
    n6.put ("age", 30);
    
    assertFalse (expr2.evaluate (n6));
    
    Notification n7 = new Notification ();
    n7.put ("hello", "there");
    
    assertEquals (BOTTOM, expr2.evaluate (n7));
    
    // opaque data
    Notification n8 = new Notification ();
    n8.put ("name", new byte [] {1, 2, 3});
    n8.put ("age", 30);
    
    assertEquals (BOTTOM, expr2.evaluate (n8));
  }
  
  @Test
  public void functions ()
    throws Exception
  {
    Notification ntfn;
    
    // basic string comparison predicates
    testPred (StrBeginsWith.class, "foobar", "foo", TRUE);
    testPred (StrBeginsWith.class, "foobar", "frob", FALSE);
    testPred (StrBeginsWith.class, null, "frob", BOTTOM);
    
    testPred (StrEndsWith.class, "foobar", "bar", TRUE);
    testPred (StrEndsWith.class, "foobar", "frob", FALSE);
    testPred (StrEndsWith.class, null, "frob", BOTTOM);
    
    testPred (StrContains.class, "foobar", "oob", TRUE);
    testPred (StrContains.class, "foobar", "frob", FALSE);
    testPred (StrContains.class, null, "frob", BOTTOM);
    
    testPred (StrRegex.class, "foobar", "o+", TRUE);
    testPred (StrRegex.class, "foobar", "o+x", FALSE);
    testPred (StrRegex.class, null, "o+", BOTTOM);
    
    testPred (StrWildcard.class, "foobar", "fo*a?", TRUE);
    testPred (StrWildcard.class, "foobar", "fo*a", FALSE);
    testPred (StrWildcard.class, null, "fo*a?", BOTTOM);
    
    // require
    ntfn = new Notification ();
    ntfn.put ("exists", "true");
    assertEquals (TRUE, new Require ("exists").evaluate (ntfn));
    assertEquals (BOTTOM, new Require ("not_exists").evaluate (ntfn));
    
    // size
    ntfn = new Notification ();
    ntfn.put ("opaque", new byte [10]);
    ntfn.put ("string", "1234");
    ntfn.put ("int32", 1234);
    assertEquals (10, new Size ("opaque").evaluate (ntfn));
    assertEquals (4, new Size ("string").evaluate (ntfn));
    assertEquals (BOTTOM, new Size ("int32").evaluate (ntfn));
    assertEquals (BOTTOM, new Size ("not_exists").evaluate (ntfn));
    
    // fold-case
    assertEquals ("hello",
                  new StrFoldCase (new Const<String> ("HellO")).evaluate (EMPTY_NOTIFICATION));
    assertEquals (BOTTOM,
                  new StrFoldCase (new Const<String> (null)).evaluate (EMPTY_NOTIFICATION));
    
    // decompose
    // see examples at http://unicode.org/reports/tr15/#Canonical_Composition_Examples
    assertEquals ("\u0041\u0301",
                  new StrUnicodeDecompose
                    (new Const<String> ("\u00C1"),
                     DECOMPOSE).evaluate (EMPTY_NOTIFICATION));
    assertEquals ("A\u0308\uFB03n",
                  new StrUnicodeDecompose
                    (new Const<String> ("\u00C4\uFB03n"),
                     DECOMPOSE).evaluate (EMPTY_NOTIFICATION));
    assertEquals ("A\u0308ffin",
                  new StrUnicodeDecompose
                    (new Const<String> ("\u00C4\uFB03n"),
                     DECOMPOSE_COMPAT).evaluate (EMPTY_NOTIFICATION));
    
    // nan
    ntfn = new Notification ();
    ntfn.put ("nan", NaN);
    ntfn.put ("notnan", 42.0);
    ntfn.put ("notnan_int", 42);
    
    assertEquals (TRUE,
                  new Nan (new Field<Double> ("nan")).evaluate (ntfn));
    assertEquals (FALSE,
                  new Nan (new Field<Double> ("notnan")).evaluate (ntfn));
    assertEquals (BOTTOM,
                  new Nan (new Field<Double> ("notnan_int")).evaluate (ntfn));
    assertEquals (BOTTOM,
                  new Nan (new Field<Double> ("nonexistent")).evaluate (ntfn));
    
    // type checks
    ntfn = new Notification ();
    ntfn.put ("int32", 1);
    ntfn.put ("int64", 2L);
    ntfn.put ("real64", 42.0);
    ntfn.put ("string", "hello");
    ntfn.put ("opaque", new byte [] {1, 2, 3});
    
    assertEquals (TRUE,
                  new Type (new Field<Integer> ("int32"), Integer.class).evaluate (ntfn));
    assertEquals (TRUE,
                 new Type (new Field<Long> ("int64"), Long.class).evaluate (ntfn));
    assertEquals (TRUE,
                 new Type (new Field<Double> ("real64"), Double.class).evaluate (ntfn));
    assertEquals (TRUE,
                 new Type (new Field<String> ("string"), String.class).evaluate (ntfn));
    assertEquals (TRUE,
                  new Type (new Field<byte []> ("opaque"), byte [].class).evaluate (ntfn));
    assertEquals (FALSE,
                  new Type (new Field<String> ("string"), Integer.class).evaluate (ntfn));
    assertEquals (BOTTOM,
                  new Type (new Field<String> ("nonexistent"), String.class).evaluate (ntfn));
  }
  
  /**
   * Check that variable-type expressions like equals (name, "foobar",
   * 42) work.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void compareMultitype ()
  {
    Notification ntfn = new Notification ();
    ntfn.put ("name", "foobar");
    
    Node<Boolean> node = Compare.create (argsList (new Field<String> ("name"),
                                                   Const.string ("foobar"),
                                                   Const.int32 (42)));
    assertEquals (TRUE, node.evaluate (ntfn));
    
    ntfn.put ("name", 42);
    assertEquals (TRUE, node.evaluate (ntfn));
    
    ntfn.put ("name", new byte [] {1, 2, 3});
    assertEquals (BOTTOM, node.evaluate (ntfn));
  }
  
  @Test
  public void mathOps ()
    throws Exception
  {
    testMathOp (MathMinus.class, 20 - 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathMinus.class, 20L - 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathMinus.class, 10.5 - 20.25, Const.real64 (10.5), Const.real64 (20.25));
    testMathOp (MathMinus.class, 10 - 20.25, Const.int32 (10), Const.real64 (20.25));
    testMathOp (MathMinus.class, null, new Field<String> ("string"), Const.real64 (20.25));
    testMathOp (MathMinus.class, null, Const.int32 (10), new Field<Number> (""));
    
    testMathOp (MathPlus.class, 20 + 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathPlus.class, 20L + 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathPlus.class, 10.5 + 20.25, Const.real64 (10.5), Const.real64 (20.25));
    testMathOp (MathPlus.class, 10 + 20.25, Const.int32 (10), Const.real64 (20.25));
    
    testMathOp (MathMult.class, 20 * 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathMult.class, 20L * 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathMult.class, 10.5 * 20.25, Const.real64 (10.5), Const.real64 (20.25));
    testMathOp (MathMult.class, 10 * 20.25, Const.int32 (10), Const.real64 (20.25));
    
    testMathOp (MathDiv.class, 20 / 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathDiv.class, 20L / 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathDiv.class, 10.5 / 20.25, Const.real64 (10.5), Const.real64 (20.25));
    testMathOp (MathDiv.class, 10 / 20.25, Const.int32 (10), Const.real64 (20.25));
    
    // div by 0
    testMathOp (MathDiv.class, BOTTOM, Const.int32 (10), Const.int32 (0));
    testMathOp (MathDiv.class, BOTTOM, Const.int64 (10), Const.int64 (0));
    testMathOp (MathDiv.class, Double.POSITIVE_INFINITY, Const.real64 (10), Const.real64 (0));
    
    testMathOp (MathMod.class, BOTTOM, Const.int32 (20), Const.int32 (0));
    testMathOp (MathMod.class, Double.NaN, Const.real64 (20), Const.real64 (0));

    // modulo div
    testMathOp (MathMod.class, 20 % 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathMod.class, 20L % 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathMod.class, 10.5 % 20.25, Const.real64 (10.5), Const.real64 (20.25));
    testMathOp (MathMod.class, 10 % 20.25, Const.int32 (10), Const.real64 (20.25));
    
    // bitwise ops
    testMathOp (MathBitAnd.class, 20 & 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathBitAnd.class, 20L & 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathBitAnd.class, 20L & 30, Const.int64 (20), Const.int32 (30));
    
    testMathOp (MathBitOr.class, 20 | 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathBitOr.class, 20L | 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathBitOr.class, 20L | 30, Const.int64 (20), Const.int32 (30));
    
    testMathOp (MathBitXor.class, 20 ^ 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathBitXor.class, 20L ^ 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathBitXor.class, 20L ^ 30, Const.int64 (20), Const.int32 (30));
    
    testMathOp (MathBitShiftLeft.class, 20 << 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathBitShiftLeft.class, 20L << 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathBitShiftLeft.class, 20L << 30, Const.int64 (20), Const.int32 (30));
    
    testMathOp (MathBitShiftRight.class, 20 >> 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathBitShiftRight.class, 20L >> 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathBitShiftRight.class, 20L >> 30, Const.int64 (20), Const.int32 (30));
    
    testMathOp (MathBitLogShiftRight.class, 20 >>> 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathBitLogShiftRight.class, 20L >>> 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathBitLogShiftRight.class, 20L >>> 30, Const.int64 (20), Const.int32 (30));
    
    // bitwise complement ~
    Notification ntfn = new Notification ();
    ntfn.put ("string", "string");
    
    MathBitInvert invert;
    
    invert = new MathBitInvert (Const.int32 (10));
    assertEquals (~10, invert.evaluate (ntfn));
    
    invert = new MathBitInvert (Const.int64 (1234567890L));
    assertEquals (~1234567890L, invert.evaluate (ntfn));
    
    invert = new MathBitInvert (new Field<Integer> ("string"));
    assertEquals (null, invert.evaluate (ntfn));
  }
  
  /**
   * Test that the Compare node handles upconverting numeric children.
   */
  @Test
  public void numericPromotion ()
  {
    Const<Long> thirtyTwoLong = new Const<Long> (32L);
    Const<Integer> thirtyTwoInt = new Const<Integer> (32);
    Const<Integer> tenInt = new Const<Integer> (10);
    Const<Double> piDouble = new Const<Double> (3.1415);
    
    Compare compare;
   
    // 32L < 10
    compare = new Compare (thirtyTwoLong, tenInt, -1, false);
    
    assertEquals (FALSE, compare.evaluate (new Notification ()));
    
    // 10 > 32L
    compare = new Compare (tenInt, thirtyTwoLong, -1, false);
    
    assertEquals (TRUE, compare.evaluate (new Notification ()));
    
    // 10 > pi
    compare = new Compare (tenInt, piDouble, 1, false);
    
    assertEquals (TRUE, compare.evaluate (new Notification ()));
    
    // 32 > pi
    compare = new Compare (thirtyTwoLong, piDouble, 1, false);
    
    assertEquals (TRUE, compare.evaluate (new Notification ()));
    
    //  32 == 32L
    compare = new Compare (thirtyTwoInt, thirtyTwoLong, 0, true);
    
    assertEquals (TRUE, compare.evaluate (new Notification ()));
  }
  
  /**
   * Test inlining of constant sub-expressions. NOTE: this depends on
   * the parser to generate AST's.
   */
  @Test
  public void constantExpressions ()
    throws ParseException
  {
    // test reduction to constant
    assertReducesTo ("1 == 1", "(constant 'true')");
    assertReducesTo ("1 != 1", "(constant 'false')");
    assertReducesTo ("1 != 1", "(constant 'false')");
    assertReducesTo ("10 > 9", "(constant 'true')");
    assertReducesTo ("!(10 > 9)", "(constant 'false')");
    assertReducesTo ("1 == 1 ^^ 10 > 9", "(constant 'false')");
    assertReducesTo ("1 != 1 || 2 != 2 || 3 != 3", "(constant 'false')");
    assertReducesTo ("1 == 1 && 2 == 2 && 3 == 3", "(constant 'true')");

    // test AND/OR erasure of constant non-contributing subtree
    assertReducesTo ("field == 5 && 10 > 9", "(== (field 'field') (int32 5))");
    assertReducesTo ("field == 5 || 10 < 9", "(== (field 'field') (int32 5))");
    assertReducesTo ("field == 5 || !(10 > 9)", "(== (field 'field') (int32 5))");

    // test redundant constants are removed from AND/OR
    assertReducesTo ("field == 5 || 9 > 10 || field == 10",
                     "(|| (== (field 'field') (int32 5)) (== (field 'field') (int32 10)))");
    assertReducesTo ("field == 5 && 10 > 9 && field == 10",
                     "(&& (== (field 'field') (int32 5)) (== (field 'field') (int32 10)))");
    
    // predicate functions
    assertReducesTo ("fold-case ('HellO')", "(string 'hello')");
    assertReducesTo ("decompose ('\u00C4\uFB03n')", "(string 'A\u0308\uFB03n')");
    assertReducesTo ("decompose-compat ('\u00C4\uFB03n')", "(string 'A\u0308ffin')");
    
    // some math ops
    assertReducesTo ("-10", "(int32 -10)");
    assertReducesTo ("1 + 1", "(int32 2)");
    assertReducesTo ("2 * 4.5", "(real64 9.0)");
    assertReducesTo ("0xFF & 0xF0", "(int32 240)");
    assertReducesTo ("0x0F << 4", "(int32 240)");
  }
  
  /**
   * Test a math operator node.
   * 
   * @param opType The operator type.
   * @param correct The correct answer.
   * @param number1 Number parameter 1
   * @param number2 Number parameter 1
   */
  private static void testMathOp (Class<? extends Node> opType, Object correct,
                                  Node number1, Node number2)
    throws Exception
  {
    Node<?> op =
      opType.getConstructor (Node.class, Node.class).newInstance (number1, number2);
    
    Notification ntfn = new Notification ();
    ntfn.put ("string", "string");
    
    assertEquals (correct, op.evaluate (ntfn));
  }
  
  /**
   * Create a list from an array of nodes.
   */
  private static List<Node<? extends Comparable>>
    argsList (Node<? extends Comparable>... args)
  {
    return Arrays.asList (args);
  }

  /**
   * Test a predicate operator node.
   * 
   * @param type The node type.
   * @param arg1 Argument 1.
   * @param arg2 Argument 2.
   * @param answer The correct answer.
   */
  private static void testPred (Class<? extends StringCompareNode> type,
                                String arg1, String arg2, Boolean answer)
    throws Exception
  {
    Node<Boolean> node =
      type.getConstructor (Node.class, Const.class).newInstance
        (new Const<String> (arg1), new Const<String> (arg2));
    
    assertEquals (answer, node.evaluate (EMPTY_NOTIFICATION));
  }

  private static void assertReducesTo (String subExpr, String treeExpr)
    throws ParseException
  {
    // System.out.println ("tree: " +
    //                     Nodes.unparse (parse (subExpr).expandConstants ()));
    assertEquals (treeExpr,
                  Nodes.unparse (parse (subExpr).inlineConstants ()));
  }
  
  private static Node parse (String expr)
    throws org.avis.pubsub.parser.ParseException
  {
    return new SubscriptionParser (new StringReader (expr)).parse ();
  }
  
  /**
   * Check that an operator matches its truth table entry.
   * 
   * @param opType Operator type.
   * @param a Left value
   * @param b Right value
   * @param correct Correct result
   */
  private static <NODE extends Node<Boolean>>
    void checkTruthTable (Class<NODE> opType,
                          Boolean a, Boolean b,
                          Boolean correct)
    throws Exception
  {
    NODE node = newLogicNodeInstance (opType, a, b);
    Boolean result = node.evaluate (EMPTY_NOTIFICATION);
    
    assertEquals (String.format
                    ("Truth table check failed: " +
                     "%1$s (\"%2$s\", \"%3$s\") == \"%4$s\" " +
                     "(should be \"%5$s\")",
                     node.name (), a, b, result, correct),
                  correct, result);
  }
  
  /**
   * Create a new node instance for nodes taking two fixed value
   * boolean children.
   */
  private static <NODE extends Node<Boolean>>
    NODE newLogicNodeInstance (Class<NODE> nodeType, Boolean a, Boolean b)
    throws Exception
  {
    Constructor<NODE> c =
      nodeType.getConstructor (Node.class, Node.class);
    
    return c.newInstance (new Const<Boolean> (a), new Const<Boolean> (b));
  }

  /**
   * Use the Compare node to compare two numbers.
   */
  private Boolean compare (int n1, int n2, int inequality, boolean equality)
  {
    return new Compare
      (new Const<Integer> (n1),
       new Const<Integer> (n2),
       inequality, equality).evaluate (EMPTY_NOTIFICATION);
  }
  
 /**
  * Generate an AST that tests AND, OR, NOT, plus various comparisons
  * 
  * name == "Matt" && (age < 20 || age >= 50) && ! blah == "frob"
  */
  @SuppressWarnings("unchecked")
  private static Node<Boolean> logicOpExpr1 ()
  {
    return new And
      (new Compare
        (new Field<String> ("name"), new Const<String> ("Matt"), 0, true),
       new Or
         (new Compare
           (new Field<Integer> ("age"), new Const<Integer> (20), -1, false),
          new Compare
           (new Field<Integer> ("age"), new Const<Integer> (50), 1, true)),
       new Not
         (new Compare (new Field<String> ("blah"), new Const<String> ("frob"), 0, true)));
  }

 /**
  * Generate an AST that tests XOR.
  *
  * name == "Matt" ^^ age == 30";
  */
  private static Node<Boolean> logicOpExpr2 ()
  {
    return new Xor
      (new Compare
        (new Field<String> ("name"), new Const<String> ("Matt"), 0, true),
       new Compare
        (new Field<Integer> ("age"), new Const<Integer> (30), 0, true));
  }
}
