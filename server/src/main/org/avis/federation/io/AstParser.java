package org.avis.federation.io;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.subscription.ast.IllegalChildException;
import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.nodes.And;
import org.avis.subscription.ast.nodes.Compare;
import org.avis.subscription.ast.nodes.Const;
import org.avis.subscription.ast.nodes.Field;
import org.avis.subscription.ast.nodes.MathBitAnd;
import org.avis.subscription.ast.nodes.MathBitInvert;
import org.avis.subscription.ast.nodes.MathBitLogShiftRight;
import org.avis.subscription.ast.nodes.MathBitOr;
import org.avis.subscription.ast.nodes.MathBitShiftLeft;
import org.avis.subscription.ast.nodes.MathBitShiftRight;
import org.avis.subscription.ast.nodes.MathBitXor;
import org.avis.subscription.ast.nodes.MathDiv;
import org.avis.subscription.ast.nodes.MathMinus;
import org.avis.subscription.ast.nodes.MathMod;
import org.avis.subscription.ast.nodes.MathMult;
import org.avis.subscription.ast.nodes.MathPlus;
import org.avis.subscription.ast.nodes.MathUnaryMinus;
import org.avis.subscription.ast.nodes.Nan;
import org.avis.subscription.ast.nodes.Not;
import org.avis.subscription.ast.nodes.Or;
import org.avis.subscription.ast.nodes.Require;
import org.avis.subscription.ast.nodes.Size;
import org.avis.subscription.ast.nodes.StrBeginsWith;
import org.avis.subscription.ast.nodes.StrContains;
import org.avis.subscription.ast.nodes.StrEndsWith;
import org.avis.subscription.ast.nodes.StrFoldCase;
import org.avis.subscription.ast.nodes.StrRegex;
import org.avis.subscription.ast.nodes.StrUnicodeDecompose;
import org.avis.subscription.ast.nodes.StrWildcard;
import org.avis.subscription.ast.nodes.Type;
import org.avis.subscription.ast.nodes.Xor;

import static org.avis.federation.io.AstType.ADD;
import static org.avis.federation.io.AstType.AND;
import static org.avis.federation.io.AstType.BEGINS_WITH;
import static org.avis.federation.io.AstType.BIT_AND;
import static org.avis.federation.io.AstType.BIT_NEGATE;
import static org.avis.federation.io.AstType.BIT_OR;
import static org.avis.federation.io.AstType.BIT_XOR;
import static org.avis.federation.io.AstType.CONST_INT32;
import static org.avis.federation.io.AstType.CONST_INT64;
import static org.avis.federation.io.AstType.CONST_REAL64;
import static org.avis.federation.io.AstType.CONST_STRING;
import static org.avis.federation.io.AstType.CONTAINS;
import static org.avis.federation.io.AstType.DECOMPOSE;
import static org.avis.federation.io.AstType.DECOMPOSE_COMPAT;
import static org.avis.federation.io.AstType.DIVIDE;
import static org.avis.federation.io.AstType.EMPTY;
import static org.avis.federation.io.AstType.ENDS_WITH;
import static org.avis.federation.io.AstType.EQUALS;
import static org.avis.federation.io.AstType.FOLD_CASE;
import static org.avis.federation.io.AstType.F_EQUALS;
import static org.avis.federation.io.AstType.GREATER_THAN;
import static org.avis.federation.io.AstType.GREATER_THAN_EQUALS;
import static org.avis.federation.io.AstType.INT32;
import static org.avis.federation.io.AstType.INT64;
import static org.avis.federation.io.AstType.LESS_THAN;
import static org.avis.federation.io.AstType.LESS_THAN_EQUALS;
import static org.avis.federation.io.AstType.LOGICAL_SHIFT_RIGHT;
import static org.avis.federation.io.AstType.MODULO;
import static org.avis.federation.io.AstType.MULTIPLY;
import static org.avis.federation.io.AstType.NAME;
import static org.avis.federation.io.AstType.NAN;
import static org.avis.federation.io.AstType.NOT;
import static org.avis.federation.io.AstType.NOT_EQUALS;
import static org.avis.federation.io.AstType.OPAQUE;
import static org.avis.federation.io.AstType.OR;
import static org.avis.federation.io.AstType.PRIMARY;
import static org.avis.federation.io.AstType.REAL64;
import static org.avis.federation.io.AstType.REGEX;
import static org.avis.federation.io.AstType.REGEXP;
import static org.avis.federation.io.AstType.REQUIRE;
import static org.avis.federation.io.AstType.SECONDARY;
import static org.avis.federation.io.AstType.SHIFT_LEFT;
import static org.avis.federation.io.AstType.SHIFT_RIGHT;
import static org.avis.federation.io.AstType.SIZE;
import static org.avis.federation.io.AstType.STRING;
import static org.avis.federation.io.AstType.SUBTRACT;
import static org.avis.federation.io.AstType.TERTIARY;
import static org.avis.federation.io.AstType.TO_UPPER;
import static org.avis.federation.io.AstType.UNARY_MINUS;
import static org.avis.federation.io.AstType.UNARY_PLUS;
import static org.avis.federation.io.AstType.WILDCARD;
import static org.avis.federation.io.AstType.XOR;
import static org.avis.io.XdrCoding.TYPE_INT32;
import static org.avis.io.XdrCoding.TYPE_INT64;
import static org.avis.io.XdrCoding.TYPE_REAL64;
import static org.avis.io.XdrCoding.TYPE_STRING;
import static org.avis.io.XdrCoding.getString;
import static org.avis.subscription.ast.nodes.Const.CONST_FALSE;
import static org.avis.util.Text.className;

/**
 * Parser class for translating XDR-encoded AST's into Node-based AST's.
 * 
 * @see AstXdrCoding#decodeAST(ByteBuffer)
 *
 * @author Matthew Phillips
 */
class AstParser
{
  private ByteBuffer in;

  public AstParser (ByteBuffer in)
  {
    this.in = in;
  }

  /**
   * Read any AST expression.
   * 
   * @return The root node of the expression.
   * 
   * @throws ProtocolCodecException if an error occurs in decoding the
   *                 AST.
   * @throws IllegalChildException if a child in the AST is not a
   *                 valid type, i.e. AST is syntactically invalid.
   */
  public Node expr ()
    throws ProtocolCodecException, IllegalChildException
  {
    int type = in.getInt ();
    
    if (type == EMPTY)
      return CONST_FALSE;
    
    int leafType = in.getInt ();
    
    // sanity check leaf type for parent nodes
    if (type > CONST_STRING && leafType != 0)
    {
      throw new ProtocolCodecException
        ("Invalid leaf type for parent node: " + leafType);
    }
    
    switch (type) 
    {
      case NAME:
      case CONST_INT32:
      case CONST_INT64:
      case CONST_REAL64:
      case CONST_STRING:
        return leaf (type, leafType);
      case REGEXP:
        return StrRegex.create (children ());
      case EQUALS:
        return new Compare (binary ().expr (), expr (),  0, true);
      case NOT_EQUALS:
        return new Not (new Compare (binary ().expr (), expr (),  0, true));
      case LESS_THAN:
        return new Compare (binary ().expr (), expr (), -1, false);
      case LESS_THAN_EQUALS:
        return new Compare (binary ().expr (), expr (), -1, true);
      case GREATER_THAN:
        return new Compare (binary ().expr (), expr (),  1, false);
      case GREATER_THAN_EQUALS:
        return new Compare (binary ().expr (), expr (),  1, true);
      case OR:
        return new Or (children ());
      case XOR:
        return new Xor (children ());
      case AND:
        return new And (children ());
      case NOT:
        return new Not (single ().expr ());
      case UNARY_PLUS:
        return single ().expr ();
      case UNARY_MINUS:
        return new MathUnaryMinus (single ().expr ());
      case MULTIPLY:
        return new MathMult (binary ().expr (), expr ());
      case DIVIDE:
        return new MathDiv (binary ().expr (), expr ());
      case MODULO:
        return new MathMod (binary ().expr (), expr ());
      case ADD:
        return new MathPlus (binary ().expr (), expr ());
      case SUBTRACT:
        return new MathMinus (binary ().expr (), expr ());
      case SHIFT_LEFT:
        return new MathBitShiftLeft (binary ().expr (), expr ());
      case SHIFT_RIGHT:
        return new MathBitShiftRight (binary ().expr (), expr ());
      case LOGICAL_SHIFT_RIGHT:
        return new MathBitLogShiftRight (binary ().expr (), expr ());
      case BIT_AND:
        return new MathBitAnd (binary ().expr (), expr ());
      case BIT_XOR:
        return new MathBitXor (binary ().expr (), expr ());
      case BIT_OR:
        return new MathBitOr (binary ().expr (), expr ());
      case BIT_NEGATE:
        return new MathBitInvert (single ().expr ());
      case INT32:
        return new Type (single ().field (), Integer.class);
      case INT64:
        return new Type (single ().field (), Long.class);
      case REAL64:
        return new Type (single ().field (), Double.class);
      case STRING:
        return new Type (single ().field (), String.class);
      case OPAQUE:
        return new Type (single ().field (), byte [].class);
      case NAN:
        return new Nan (single ().field ());
      case BEGINS_WITH:
        return StrBeginsWith.create (children ());
      case CONTAINS:
        return StrContains.create (children ());
      case ENDS_WITH:
        return StrEndsWith.create (children ());
      case WILDCARD:
        return StrWildcard.create (children ());
      case REGEX:
        return StrRegex.create (children ());
      case FOLD_CASE:
        return new StrFoldCase (single ().expr ());
      case DECOMPOSE:
        return new StrUnicodeDecompose (single ().expr (), 
                                        StrUnicodeDecompose.DECOMPOSE);
      case DECOMPOSE_COMPAT:
        return new StrUnicodeDecompose (single ().expr (), 
                                        StrUnicodeDecompose.DECOMPOSE_COMPAT);
      case REQUIRE:
        return new Require (single ().field ());
      case F_EQUALS:
        return Compare.createEquals (children ());
      case SIZE:
        return new Size (single ().field ());
      default:
        throw new ProtocolCodecException 
          ("Unknown AST node type: " + type);
    }
  }

  /**
   * Assert that a single child is found and return this, otherwise
   * throw an exception. Used as a predicate for single-child nodes.
   */
  private AstParser single ()
    throws ProtocolCodecException
  {
    int count = in.getInt ();
    
    if (count == 1)
      return this;
    else
      throw new ProtocolCodecException 
        ("Expected single child, found " + count);
  }
  
  /**
   * Assert that two children are found and return this, otherwise
   * throw an exception. Used as a predicate for binary nodes.
   */
  private AstParser binary ()
    throws ProtocolCodecException
  {
    int count = in.getInt ();
    
    if (count == 2)
      return this;
    else
      throw new ProtocolCodecException 
        ("Expected two children, found " + count);
  }
  
  /**
   * Read a list of child nodes of any length.
   */
  private List<Node> children ()
    throws ProtocolCodecException
  {
    int count = in.getInt ();
    
    List<Node> children = new ArrayList<Node> (count);
    
    for ( ; count > 0; count--)
      children.add (expr ());
    
    return children;
  }
  
  private Field field ()
    throws ProtocolCodecException
  {
    Node node = expr ();
    
    if (node instanceof Field)
      return (Field)node;
    else
      throw new ProtocolCodecException ("Field node required, found " + 
                                        className (node));
  }
  
  /**
   * Read a leaf node of a given type and leaf type.
   * 
   * @param type The node type.
   * @param leafType The type of value contained in the leaf: must be
   *                one of the XdrCoding.TYPE_* values.
   * @return The node.
   * 
   * @throws ProtocolCodecException if the node was not valid constant
   *                 node of the specified type.
   */
  private Node leaf (int type, int leafType)
    throws ProtocolCodecException
  {
    switch (type)
    {
      case NAME:
        assertLeafType (leafType, TYPE_STRING);
        return new Field (getString (in));
      case CONST_STRING:
        assertLeafType (leafType, TYPE_STRING);
        return new Const (getString (in));
      case CONST_INT32:
        assertLeafType (leafType, TYPE_INT32);
        return new Const (in.getInt ());
      case CONST_INT64:
        assertLeafType (leafType, TYPE_INT64);
        return new Const (in.getLong ());
      case CONST_REAL64:
        assertLeafType (leafType, TYPE_REAL64);
        return new Const (in.getDouble ());
      default:
        throw new Error ();
    }
  }

  /**
   * Check that a node type matches an actual required value.
   */
  private static void assertLeafType (int required, int actual)
    throws ProtocolCodecException
  {
    if (required != actual)
    {
      throw new ProtocolCodecException 
        ("Leaf node has incorrect value type: " + actual);
    }
  }
}
