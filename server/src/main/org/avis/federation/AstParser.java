package org.avis.federation;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.XdrCoding;
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

import static org.avis.util.Text.className;

class AstParser
{
  private ByteBuffer in;

  public AstParser (ByteBuffer in)
  {
    this.in = in;
  }

  public Node expr ()
    throws ProtocolCodecException
  {
    AstType nodeType = nodeType (in.getInt ());
    int subType = in.getInt ();
    
    if (nodeType.ordinal () > AstType.TYPE_STRING.ordinal () && subType != 0)
    {
      throw new ProtocolCodecException ("Invalid subtype for parent node: " + 
                                        subType);
    }
    
    switch (nodeType) 
    {
      case TYPE_INT32:
      case TYPE_INT64:
      case TYPE_REAL64:
      case TYPE_STRING:
        return constant (nodeType, subType);
      case NAME:
        return new Field (single ().string ());
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
        return new Type (single ().string (), Integer.class);
      case INT64:
        return new Type (single ().string (), Long.class);
      case REAL64:
        return new Type (single ().string (), Double.class);
      case STRING:
        return new Type (single ().string (), String.class);
      case OPAQUE:
        return new Type (single ().string (), byte [].class);
      case NAN:
        return new Nan (single ().string ());
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
      case TO_LOWER:
        return new StrFoldCase (single ().expr ());
      case TO_UPPER:
        throw new ProtocolCodecException ("toupper () is not supported");
      case PRIMARY:
        throw new ProtocolCodecException ("primary is not supported");
      case SECONDARY:
        throw new ProtocolCodecException ("secondary is not supported");
      case TERTIARY:
        throw new ProtocolCodecException ("tertiary is not supported");
      case DECOMPOSE:
        return new StrUnicodeDecompose (single ().expr (), 
                                        StrUnicodeDecompose.DECOMPOSE);
      case DECOMPOSE_COMPAT:
        return new StrUnicodeDecompose (single ().expr (), 
                                        StrUnicodeDecompose.DECOMPOSE_COMPAT);
      case REQUIRE:
        return new Require (single ().string ());
      case F_EQUALS:
        return Compare.createEquals (children ());
      case SIZE:
        return new Size (single ().string ());
      default:
        throw new Error ();
    }
  }
  
  private static AstType nodeType (int nodeType)
  {
    try
    {
      return AstType.values () [nodeType];
    } catch (ArrayIndexOutOfBoundsException ex)
    {
      throw new IllegalArgumentException ("Invalid AST node type:" + nodeType);
    }
  }

  private AstParser single ()
    throws ProtocolCodecException
  {
    int count = in.getInt ();
    
    if (count == 1)
      return this;
    else
      throw new ProtocolCodecException ("Expected single child: found " + count);
  }
  
  private AstParser binary ()
    throws ProtocolCodecException
  {
    int count = in.getInt ();
    
    if (count == 2)
      return this;
    else
      throw new ProtocolCodecException ("Expected two children: found " + count);
  }
  
  private List<Node> children ()
    throws ProtocolCodecException
  {
    int count = in.getInt ();
    
    List<Node> children = new ArrayList<Node> (count);
    
    for ( ; count > 0; count--)
      children.add (expr ());
    
    return children;
  }
  
  private String string ()
    throws ProtocolCodecException
  {
    AstType nodeType = nodeType (in.getInt ());
    
    if (nodeType != AstType.TYPE_STRING)
      throw new ProtocolCodecException ("String node required, found " + nodeType);
    
    Object value = XdrCoding.getObject (in);
    
    if (value instanceof String)
      return (String)value;
    else
      throw new ProtocolCodecException ("String expected, found " + 
                                        className (value));
  }
  
  private Const constant (AstType nodeType, int subType)
    throws ProtocolCodecException
  {
    switch (subType)
    {
      case XdrCoding.TYPE_INT32:
        assertNodeType (nodeType, AstType.TYPE_INT32);
        return new Const (in.getInt ());
      case XdrCoding.TYPE_INT64:
        assertNodeType (nodeType, AstType.TYPE_INT64);
        return new Const (in.getLong ());
      case XdrCoding.TYPE_REAL64:
        assertNodeType (nodeType, AstType.TYPE_REAL64);
        return new Const (in.getDouble ());
      case XdrCoding.TYPE_STRING:
        assertNodeType (nodeType, AstType.TYPE_STRING);
        return new Const (XdrCoding.getString (in));
      default:
        throw new ProtocolCodecException ("Invalid subtype: " + subType);
    }
  }

  private static void assertNodeType (AstType required, AstType actual)
    throws ProtocolCodecException
  {
    if (required != actual)
    {
      throw new ProtocolCodecException 
        ("Constant tode " + actual + " has wrong sub type");
    }
  }
}
