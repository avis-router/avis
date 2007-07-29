package org.avis.federation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

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

import static org.avis.federation.AstType.ADD;
import static org.avis.federation.AstType.AND;
import static org.avis.federation.AstType.BEGINS_WITH;
import static org.avis.federation.AstType.BIT_AND;
import static org.avis.federation.AstType.BIT_NEGATE;
import static org.avis.federation.AstType.CONST_INT32;
import static org.avis.federation.AstType.CONST_INT64;
import static org.avis.federation.AstType.CONST_REAL64;
import static org.avis.federation.AstType.CONST_STRING;
import static org.avis.federation.AstType.CONTAINS;
import static org.avis.federation.AstType.DECOMPOSE;
import static org.avis.federation.AstType.DECOMPOSE_COMPAT;
import static org.avis.federation.AstType.DIVIDE;
import static org.avis.federation.AstType.ENDS_WITH;
import static org.avis.federation.AstType.F_EQUALS;
import static org.avis.federation.AstType.GREATER_THAN;
import static org.avis.federation.AstType.GREATER_THAN_EQUALS;
import static org.avis.federation.AstType.INT32;
import static org.avis.federation.AstType.INT64;
import static org.avis.federation.AstType.LESS_THAN;
import static org.avis.federation.AstType.LESS_THAN_EQUALS;
import static org.avis.federation.AstType.LOGICAL_SHIFT_RIGHT;
import static org.avis.federation.AstType.MODULO;
import static org.avis.federation.AstType.MULTIPLY;
import static org.avis.federation.AstType.NAME;
import static org.avis.federation.AstType.NAN;
import static org.avis.federation.AstType.NOT;
import static org.avis.federation.AstType.OPAQUE;
import static org.avis.federation.AstType.OR;
import static org.avis.federation.AstType.REAL64;
import static org.avis.federation.AstType.REGEX;
import static org.avis.federation.AstType.REQUIRE;
import static org.avis.federation.AstType.SHIFT_LEFT;
import static org.avis.federation.AstType.SHIFT_RIGHT;
import static org.avis.federation.AstType.SIZE;
import static org.avis.federation.AstType.STRING;
import static org.avis.federation.AstType.SUBTRACT;
import static org.avis.federation.AstType.TO_LOWER;
import static org.avis.federation.AstType.UNARY_MINUS;
import static org.avis.federation.AstType.WILDCARD;
import static org.avis.federation.AstType.XOR;
import static org.avis.io.XdrCoding.TYPE_INT32;
import static org.avis.io.XdrCoding.TYPE_INT64;
import static org.avis.io.XdrCoding.TYPE_REAL64;
import static org.avis.io.XdrCoding.TYPE_STRING;
import static org.avis.io.XdrCoding.putString;
import static org.avis.util.Text.className;

/**
 * Functions for encoding/decoding Node-based AST's into the Elvin XDR
 * wire format.
 * 
 * @author Matthew Phillips
 */
public final class AstXdrCoding
{
  private static Map<Class<? extends Node>, AstType> nodeToTypecode;
  
  static
  {
    nodeToTypecode = new HashMap<Class<? extends Node>, AstType> ();
    
    nodeToTypecode.put (And.class, AND);
    nodeToTypecode.put (Field.class, NAME);
    nodeToTypecode.put (MathBitAnd.class, BIT_AND);
    nodeToTypecode.put (MathBitInvert.class, BIT_NEGATE);
    nodeToTypecode.put (MathBitLogShiftRight.class, LOGICAL_SHIFT_RIGHT);
    nodeToTypecode.put (MathBitOr.class, OR);
    nodeToTypecode.put (MathBitShiftLeft.class, SHIFT_LEFT);
    nodeToTypecode.put (MathBitShiftRight.class, SHIFT_RIGHT);
    nodeToTypecode.put (MathBitXor.class, XOR);
    nodeToTypecode.put (MathDiv.class, DIVIDE);
    nodeToTypecode.put (MathMinus.class, SUBTRACT);
    nodeToTypecode.put (MathMod.class, MODULO);
    nodeToTypecode.put (MathMult.class, MULTIPLY);
    nodeToTypecode.put (MathPlus.class, ADD);
    nodeToTypecode.put (MathUnaryMinus.class, UNARY_MINUS);
    nodeToTypecode.put (Nan.class, NAN);
    nodeToTypecode.put (Not.class, NOT);
    nodeToTypecode.put (Or.class, OR);
    nodeToTypecode.put (Require.class, REQUIRE);
    nodeToTypecode.put (Size.class, SIZE);
    nodeToTypecode.put (StrBeginsWith.class, BEGINS_WITH);
    nodeToTypecode.put (StrContains.class, CONTAINS);
    nodeToTypecode.put (StrEndsWith.class, ENDS_WITH);
    nodeToTypecode.put (StrFoldCase.class, TO_LOWER);
    nodeToTypecode.put (StrRegex.class, REGEX);
    nodeToTypecode.put (StrWildcard.class, WILDCARD);
    nodeToTypecode.put (Xor.class, XOR);
  }
  
  private AstXdrCoding ()
  {
    // zip
  }
  
  /**
   * Encode an AST in Elvin XDR format.
   * 
   * @param out The buffer to encode to.
   * @param node The root of the AST.
   * 
   * @see #decodeAST(ByteBuffer)
   */
  public static void encodeAST (ByteBuffer out, Node node)
  {
    if (node instanceof Const)
    {
      encodeConst (out, (Const)node);
    } else
    {
      AstType typeCode = typeCodeFor (node);
      
      out.putInt (typeCode.ordinal ());
      out.putInt (0); // composite node base type is 0
      
      // children
      if (node.hasChildren ())
      {
        Collection<? extends Node> children = node.children ();
        
        out.putInt (children.size ());
        
        for (Node child : children)
          encodeAST (out, child);
      } else
      {
        out.putInt (0);
      }
    }
  }
  
  /**
   * Generate the AST type code for a node, taking into account cases
   * where there is not a 1-1 mapping from Node -> Elvin AST node type.
   */
  private static AstType typeCodeFor (Node node)
  {
    if (node instanceof Compare)
    {
      Compare compare = (Compare)node;
      
      switch (compare.inequality)
      {
        case 0:
          return F_EQUALS;
        case -1:
          return compare.equality ? LESS_THAN_EQUALS : LESS_THAN;
        case 1:
          return compare.equality ? GREATER_THAN_EQUALS : GREATER_THAN;
        default:
          throw new Error ();
      }
    } else if (node instanceof Type)
    {
      Class<?> type = ((Type)node).type;
      
      if (type == String.class)
        return STRING;
      else if (type == Integer.class)
        return INT32;
      else if (type == Long.class)
        return INT64;
      else if (type == Double.class)
        return REAL64;
      else if (type == byte [].class)
        return OPAQUE;
      else
        throw new Error ();
    } else if (node instanceof StrUnicodeDecompose)
    {
      StrUnicodeDecompose decompose = (StrUnicodeDecompose)node;
      
      return decompose.mode == StrUnicodeDecompose.DECOMPOSE ?
               DECOMPOSE : DECOMPOSE_COMPAT;
    } else
    {
      // sanity check is that this will NPE on unmapped type
      return nodeToTypecode.get (node.getClass ());
    }
  }

  /**
   * Encode a constant value (leaf) node.
   */
  private static void encodeConst (ByteBuffer out, Const node)
  {
    Object value = node.value ();
    Class<?> type = value.getClass ();

    if (type == String.class)
    {
      out.putInt (CONST_STRING.ordinal ());
      out.putInt (TYPE_STRING);
      putString (out, (String)value);
    } else if (type == Integer.class)
    {
      out.putInt (CONST_INT32.ordinal ());
      out.putInt (TYPE_INT32);
      out.putInt ((Integer)value);
    } else if (type == Long.class)
    {
      out.putInt (CONST_INT64.ordinal ());
      out.putInt (TYPE_INT64);
      out.putLong ((Long)value);
    } else if (type == Double.class)
    {
      out.putInt (CONST_REAL64.ordinal ());
      out.putInt (TYPE_REAL64);
      out.putDouble ((Double)value);
    } else
    {
      throw new Error ("Cannot encode constant type " + className (type));
    }
  }

  /**
   * Decode an XDR-encoded AST into a Node-based AST.
   * 
   * @param in The buffer to read from.
   * 
   * @return The root of the AST.
   * 
   * @throws ProtocolCodecException if an error occurred reading the tree.
   * 
   * @see #encodeAST(ByteBuffer, Node)
   */
  public static Node decodeAST (ByteBuffer in)
    throws ProtocolCodecException
  {
    return new AstParser (in).expr ();
  }
}
