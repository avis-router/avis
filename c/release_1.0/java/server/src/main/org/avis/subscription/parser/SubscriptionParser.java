/* Generated By:JavaCC: Do not edit this line. SubscriptionParser.java */
package org.avis.subscription.parser;

import java.util.*;
import java.util.regex.PatternSyntaxException;

import org.avis.subscription.ast.*;
import org.avis.subscription.ast.nodes.*;

@SuppressWarnings ("all")
public class SubscriptionParser extends SubscriptionParserBase implements SubscriptionParserConstants {
  Node doParse ()
    throws ParseException
  {
    return Start ();
  }

  final public Node Start() throws ParseException {
  Node node;
    node = SubExp();
    jj_consume_token(0);
    {if (true) return node;}
    throw new Error("Missing return statement in function");
  }

  final public Node SubExp() throws ParseException {
    {if (true) return BoolOrExp ();}
    throw new Error("Missing return statement in function");
  }

  final public Node BoolOrExp() throws ParseException {
  Node node1;
  Node node2;
  Or or = null;
    node1 = BoolXorExp();
    label_1:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 20:
        ;
        break;
      default:
        jj_la1[0] = jj_gen;
        break label_1;
      }
      jj_consume_token(20);
      node2 = BoolXorExp();
      if (or == null)
        node1 = or = new Or (node1);

      or.addChild (node2);
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node BoolXorExp() throws ParseException {
  Node node1;
  Node node2;
  Xor xor = null;
    node1 = BoolAndExp();
    label_2:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 21:
        ;
        break;
      default:
        jj_la1[1] = jj_gen;
        break label_2;
      }
      jj_consume_token(21);
      node2 = BoolAndExp();
      if (xor == null)
        node1 = xor = new Xor (node1);

      xor.addChild (node2);
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node BoolAndExp() throws ParseException {
  Node node1;
  Node node2;
  And and = null;
    node1 = BoolExp();
    label_3:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 22:
        ;
        break;
      default:
        jj_la1[2] = jj_gen;
        break label_3;
      }
      jj_consume_token(22);
      node2 = BoolExp();
      if (and == null)
        node1 = and = new And (node1);

      and.addChild (node2);
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node BoolExp() throws ParseException {
  Node node1;
  Node node2;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 23:
      jj_consume_token(23);
      node1 = BoolExp();
                              {if (true) return new Not (node1);}
      break;
    case IDENTIFIER:
    case INTEGER_LITERAL:
    case REAL_LITERAL:
    case STRING_LITERAL:
    case 30:
    case 31:
    case 41:
    case 42:
      node1 = MathExp();
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 24:
      case 25:
      case 26:
      case 27:
      case 28:
      case 29:
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case 24:
          jj_consume_token(24);
          node2 = MathExp();
                              {if (true) return new Compare (node1, node2,  1, false);}
          break;
        case 25:
          jj_consume_token(25);
          node2 = MathExp();
                              {if (true) return new Compare (node1, node2,  1, true);}
          break;
        case 26:
          jj_consume_token(26);
          node2 = MathExp();
                              {if (true) return new Compare (node1, node2, -1, false);}
          break;
        case 27:
          jj_consume_token(27);
          node2 = MathExp();
                              {if (true) return new Compare (node1, node2, -1, true);}
          break;
        case 28:
          jj_consume_token(28);
          node2 = MathExp();
                              {if (true) return new Compare (node1, node2,  0, true);}
          break;
        case 29:
          jj_consume_token(29);
          node2 = MathExp();
                              {if (true) return new Not (new Compare (node1, node2,  0, true));}
          break;
        default:
          jj_la1[3] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
        }
        break;
      default:
        jj_la1[4] = jj_gen;
        ;
      }
      {if (true) return node1;}
      break;
    default:
      jj_la1[5] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public Node MathExp() throws ParseException {
  Node node1;
    node1 = MathAddExp();
    label_4:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 30:
        ;
        break;
      default:
        jj_la1[6] = jj_gen;
        break label_4;
      }
      jj_consume_token(30);
            node1 = new MathMinus (node1, MathAddExp ());
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node MathAddExp() throws ParseException {
  Node node1;
    node1 = MathMultExp();
    label_5:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 31:
        ;
        break;
      default:
        jj_la1[7] = jj_gen;
        break label_5;
      }
      jj_consume_token(31);
            node1 = new MathPlus (node1, MathMultExp ());
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node MathMultExp() throws ParseException {
  Node node1;
    node1 = MathDivExp();
    label_6:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 32:
        ;
        break;
      default:
        jj_la1[8] = jj_gen;
        break label_6;
      }
      jj_consume_token(32);
            node1 = new MathMult (node1, MathDivExp ());
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node MathDivExp() throws ParseException {
  Node node1;
    node1 = MathModExp();
    label_7:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 33:
        ;
        break;
      default:
        jj_la1[9] = jj_gen;
        break label_7;
      }
      jj_consume_token(33);
            node1 = new MathDiv (node1, MathModExp ());
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node MathModExp() throws ParseException {
  Node node1;
    node1 = MathBitShiftLeftExp();
    label_8:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 34:
        ;
        break;
      default:
        jj_la1[10] = jj_gen;
        break label_8;
      }
      jj_consume_token(34);
            node1 = new MathMod (node1, MathBitShiftLeftExp ());
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node MathBitShiftLeftExp() throws ParseException {
  Node node1;
    node1 = MathBitShiftRightExp();
    label_9:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 35:
        ;
        break;
      default:
        jj_la1[11] = jj_gen;
        break label_9;
      }
      jj_consume_token(35);
             node1 = new MathBitShiftLeft (node1, MathBitShiftRightExp ());
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node MathBitShiftRightExp() throws ParseException {
  Node node1;
    node1 = MathBitLogShiftRightExp();
    label_10:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 36:
        ;
        break;
      default:
        jj_la1[12] = jj_gen;
        break label_10;
      }
      jj_consume_token(36);
             node1 = new MathBitShiftRight (node1, MathBitLogShiftRightExp ());
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node MathBitLogShiftRightExp() throws ParseException {
  Node node1;
    node1 = MathBitOrExp();
    label_11:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 37:
        ;
        break;
      default:
        jj_la1[13] = jj_gen;
        break label_11;
      }
      jj_consume_token(37);
              node1 = new MathBitLogShiftRight (node1, MathBitOrExp ());
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node MathBitOrExp() throws ParseException {
  Node node1;
    node1 = MathBitXorExp();
    label_12:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 38:
        ;
        break;
      default:
        jj_la1[14] = jj_gen;
        break label_12;
      }
      jj_consume_token(38);
            node1 = new MathBitOr (node1, MathBitXorExp ());
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node MathBitXorExp() throws ParseException {
  Node node1;
    node1 = MathBitAndExp();
    label_13:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 39:
        ;
        break;
      default:
        jj_la1[15] = jj_gen;
        break label_13;
      }
      jj_consume_token(39);
            node1 = new MathBitXor (node1, MathBitAndExp ());
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node MathBitAndExp() throws ParseException {
  Node node1;
    node1 = ValueExp();
    label_14:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 40:
        ;
        break;
      default:
        jj_la1[16] = jj_gen;
        break label_14;
      }
      jj_consume_token(40);
            node1 = new MathBitAnd (node1, ValueExp ());
    }
    {if (true) return node1;}
    throw new Error("Missing return statement in function");
  }

  final public Node ValueExp() throws ParseException {
  Node node;
    if (jj_2_1(2)) {
      // differentiate between Name and Function
      
        node = Function();
                            {if (true) return node;}
    } else {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case IDENTIFIER:
        node = Name();
                            {if (true) return node;}
        break;
      case INTEGER_LITERAL:
      case REAL_LITERAL:
        node = NumLiteral();
                            {if (true) return node;}
        break;
      case STRING_LITERAL:
        node = StringLiteral();
                            {if (true) return node;}
        break;
      case 30:
        jj_consume_token(30);
        node = ValueExp();
                            {if (true) return new MathUnaryMinus (node);}
        break;
      case 31:
        jj_consume_token(31);
        node = ValueExp();
                            {if (true) return node;}
        break;
      case 41:
        jj_consume_token(41);
        node = ValueExp();
                            {if (true) return new MathBitInvert (node);}
        break;
      case 42:
        jj_consume_token(42);
        node = SubExp();
        jj_consume_token(43);
                            {if (true) return node;}
        break;
      default:
        jj_la1[17] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
    throw new Error("Missing return statement in function");
  }

  final public Node Function() throws ParseException {
  Token t;
  Node node;
  String func;
    t = jj_consume_token(IDENTIFIER);
    jj_consume_token(42);
    func = t.image;

    if (func.equals ("begins-with"))
      node = StrBeginsWith.create (StringCompareArgs ());
    else if (func.equals ("ends-with"))
      node = StrEndsWith.create (StringCompareArgs ());
    else if (func.equals ("contains"))
      node = StrContains.create (StringCompareArgs ());
    else if (func.equals ("regex"))
      node = StrRegex.create (StringCompareArgs ());
    else if (func.equals ("fold-case"))
      node = new StrFoldCase (StringValue ());
    else if (func.equals ("equals"))
      node = Compare.createEquals (CompareArgs ());
    else if (func.equals ("require"))
      node = new Require (Name ());
    else if (func.equals ("int32"))
      node = new Type (Name (), Integer.class);
    else if (func.equals ("int64"))
      node = new Type (Name (), Long.class);
    else if (func.equals ("real64"))
      node = new Type (Name (), Double.class);
    else if (func.equals ("string"))
      node = new Type (Name (), String.class);
    else if (func.equals ("opaque"))
      node = new Type (Name (), byte [].class);
    else if (func.equals ("nan"))
      node = new Nan (Name ());
    else if (func.equals ("wildcard"))
      node = StrWildcard.create (StringCompareArgs ());
    else if (func.equals ("size"))
      node = new Size (Name ());
    else if (func.equals ("decompose") || func.equals ("decompose-compat"))
      node = new StrUnicodeDecompose
        (StringValue (),
         func.equals ("decompose") ? StrUnicodeDecompose.Mode.DECOMPOSE :
                                     StrUnicodeDecompose.Mode.DECOMPOSE_COMPAT);
    else
    {
      node = null;
      AnyArgs (); // error recover by skipping args
    }
    jj_consume_token(43);
    if (node == null)
      {if (true) throw new ParseException ("Unknown function: " + func);}
    else
      {if (true) return node;}
    throw new Error("Missing return statement in function");
  }

  final public Node StringValue() throws ParseException {
  Node node;
    if (jj_2_2(2)) {
      node = Function();
    } else {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case STRING_LITERAL:
        node = StringLiteral();
        break;
      case IDENTIFIER:
        node = Name();
        break;
      default:
        jj_la1[18] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
    {if (true) return node;}
    throw new Error("Missing return statement in function");
  }

// Args for the compare() function
  final public List CompareArgs() throws ParseException {
  ArrayList args = new ArrayList ();
    args.add (StringValue ());
    label_15:
    while (true) {
      jj_consume_token(44);
                                         args.add (MathExp ());
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 44:
        ;
        break;
      default:
        jj_la1[19] = jj_gen;
        break label_15;
      }
    }
    {if (true) return args;}
    throw new Error("Missing return statement in function");
  }

// Args for any string comparison function
  final public List StringCompareArgs() throws ParseException {
  ArrayList args = new ArrayList ();
   args.add (StringValue ());
    label_16:
    while (true) {
      jj_consume_token(44);
                                      args.add (StringLiteral ());
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 44:
        ;
        break;
      default:
        jj_la1[20] = jj_gen;
        break label_16;
      }
    }
    {if (true) return args;}
    throw new Error("Missing return statement in function");
  }

// Match any arguments. Used for error recovery
  final public void AnyArgs() throws ParseException {
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case IDENTIFIER:
    case INTEGER_LITERAL:
    case REAL_LITERAL:
    case STRING_LITERAL:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case IDENTIFIER:
      case STRING_LITERAL:
        StringValue();
        break;
      case INTEGER_LITERAL:
      case REAL_LITERAL:
        NumLiteral();
        break;
      default:
        jj_la1[21] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      label_17:
      while (true) {
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case 44:
          ;
          break;
        default:
          jj_la1[22] = jj_gen;
          break label_17;
        }
        jj_consume_token(44);
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case IDENTIFIER:
        case STRING_LITERAL:
          StringValue();
          break;
        case INTEGER_LITERAL:
        case REAL_LITERAL:
          NumLiteral();
          break;
        default:
          jj_la1[23] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
        }
      }
      break;
    default:
      jj_la1[24] = jj_gen;
      ;
    }
  }

  final public Const NumLiteral() throws ParseException {
  Token t;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case INTEGER_LITERAL:
      t = jj_consume_token(INTEGER_LITERAL);
    Number value;
    char lastChar = t.image.charAt (t.image.length () - 1);

    if (lastChar == 'l' || lastChar == 'L')
      value = Long.decode (t.image.substring (0, t.image.length () - 1));
    else
      value = Integer.decode (t.image);

    {if (true) return new Const (value);}
      break;
    case REAL_LITERAL:
      t = jj_consume_token(REAL_LITERAL);
    {if (true) return new Const (new Double (t.image));}
      break;
    default:
      jj_la1[25] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public Field Name() throws ParseException {
  Token t;
    t = jj_consume_token(IDENTIFIER);
    {if (true) return new Field (stripBackslashes (t.image));}
    throw new Error("Missing return statement in function");
  }

  final public Const StringLiteral() throws ParseException {
  Token t;
    t = jj_consume_token(STRING_LITERAL);
    {if (true) return new Const
      (stripBackslashes (t.image.substring (1, t.image.length () - 1)));}
    throw new Error("Missing return statement in function");
  }

  final private boolean jj_2_1(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_1(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(0, xla); }
  }

  final private boolean jj_2_2(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_2(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(1, xla); }
  }

  final private boolean jj_3_2() {
    if (jj_3R_18()) return true;
    return false;
  }

  final private boolean jj_3_1() {
    if (jj_3R_18()) return true;
    return false;
  }

  final private boolean jj_3R_18() {
    if (jj_scan_token(IDENTIFIER)) return true;
    if (jj_scan_token(42)) return true;
    return false;
  }

  public SubscriptionParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  public Token token, jj_nt;
  private int jj_ntk;
  private Token jj_scanpos, jj_lastpos;
  private int jj_la;
  public boolean lookingAhead = false;
  private boolean jj_semLA;
  private int jj_gen;
  final private int[] jj_la1 = new int[26];
  static private int[] jj_la1_0;
  static private int[] jj_la1_1;
  static {
      jj_la1_0();
      jj_la1_1();
   }
   private static void jj_la1_0() {
      jj_la1_0 = new int[] {0x100000,0x200000,0x400000,0x3f000000,0x3f000000,0xc0814220,0x40000000,0x80000000,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0xc0014220,0x10020,0x0,0x0,0x14220,0x0,0x14220,0x14220,0x4200,};
   }
   private static void jj_la1_1() {
      jj_la1_1 = new int[] {0x0,0x0,0x0,0x0,0x0,0x600,0x0,0x0,0x1,0x2,0x4,0x8,0x10,0x20,0x40,0x80,0x100,0x600,0x0,0x1000,0x1000,0x0,0x1000,0x0,0x0,0x0,};
   }
  final private JJCalls[] jj_2_rtns = new JJCalls[2];
  private boolean jj_rescan = false;
  private int jj_gc = 0;

  public SubscriptionParser(java.io.InputStream stream) {
     this(stream, null);
  }
  public SubscriptionParser(java.io.InputStream stream, String encoding) {
    try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source = new SubscriptionParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 26; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public void ReInit(java.io.InputStream stream) {
     ReInit(stream, null);
  }
  public void ReInit(java.io.InputStream stream, String encoding) {
    try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 26; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public SubscriptionParser(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new SubscriptionParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 26; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 26; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public SubscriptionParser(SubscriptionParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 26; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public void ReInit(SubscriptionParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 26; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  final private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      if (++jj_gc > 100) {
        jj_gc = 0;
        for (int i = 0; i < jj_2_rtns.length; i++) {
          JJCalls c = jj_2_rtns[i];
          while (c != null) {
            if (c.gen < jj_gen) c.first = null;
            c = c.next;
          }
        }
      }
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  static private final class LookaheadSuccess extends java.lang.Error { }
  final private LookaheadSuccess jj_ls = new LookaheadSuccess();
  final private boolean jj_scan_token(int kind) {
    if (jj_scanpos == jj_lastpos) {
      jj_la--;
      if (jj_scanpos.next == null) {
        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
      } else {
        jj_lastpos = jj_scanpos = jj_scanpos.next;
      }
    } else {
      jj_scanpos = jj_scanpos.next;
    }
    if (jj_rescan) {
      int i = 0; Token tok = token;
      while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
      if (tok != null) jj_add_error_token(kind, i);
    }
    if (jj_scanpos.kind != kind) return true;
    if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
    return false;
  }

  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

  final public Token getToken(int index) {
    Token t = lookingAhead ? jj_scanpos : token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  final private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.Vector<int[]> jj_expentries = new java.util.Vector<int[]>();
  private int[] jj_expentry;
  private int jj_kind = -1;
  private int[] jj_lasttokens = new int[100];
  private int jj_endpos;

  private void jj_add_error_token(int kind, int pos) {
    if (pos >= 100) return;
    if (pos == jj_endpos + 1) {
      jj_lasttokens[jj_endpos++] = kind;
    } else if (jj_endpos != 0) {
      jj_expentry = new int[jj_endpos];
      for (int i = 0; i < jj_endpos; i++) {
        jj_expentry[i] = jj_lasttokens[i];
      }
      boolean exists = false;
      for (java.util.Enumeration e = jj_expentries.elements(); e.hasMoreElements();) {
        int[] oldentry = (int[])(e.nextElement());
        if (oldentry.length == jj_expentry.length) {
          exists = true;
          for (int i = 0; i < jj_expentry.length; i++) {
            if (oldentry[i] != jj_expentry[i]) {
              exists = false;
              break;
            }
          }
          if (exists) break;
        }
      }
      if (!exists) jj_expentries.addElement(jj_expentry);
      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
    }
  }

  public ParseException generateParseException() {
    jj_expentries.removeAllElements();
    boolean[] la1tokens = new boolean[45];
    for (int i = 0; i < 45; i++) {
      la1tokens[i] = false;
    }
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 26; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
          if ((jj_la1_1[i] & (1<<j)) != 0) {
            la1tokens[32+j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 45; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.addElement(jj_expentry);
      }
    }
    jj_endpos = 0;
    jj_rescan_token();
    jj_add_error_token(0, 0);
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = (int[])jj_expentries.elementAt(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  final public void enable_tracing() {
  }

  final public void disable_tracing() {
  }

  final private void jj_rescan_token() {
    jj_rescan = true;
    for (int i = 0; i < 2; i++) {
    try {
      JJCalls p = jj_2_rtns[i];
      do {
        if (p.gen > jj_gen) {
          jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
          switch (i) {
            case 0: jj_3_1(); break;
            case 1: jj_3_2(); break;
          }
        }
        p = p.next;
      } while (p != null);
      } catch(LookaheadSuccess ls) { }
    }
    jj_rescan = false;
  }

  final private void jj_save(int index, int xla) {
    JJCalls p = jj_2_rtns[index];
    while (p.gen > jj_gen) {
      if (p.next == null) { p = p.next = new JJCalls(); break; }
      p = p.next;
    }
    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;
  }

  static final class JJCalls {
    int gen;
    Token first;
    int arg;
    JJCalls next;
  }

}
