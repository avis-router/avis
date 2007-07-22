package org.avis.federation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.ByteBuffer;

import org.avis.io.IO;
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

import static org.avis.io.IO.putString;

/**
 * Functions for encoding/decoding Node-based AST's into the Elvin XDR
 * wire format.
 * 
 * @author Matthew Phillips
 */
public final class AstIO
{
  @SuppressWarnings("unused")
  private static final int
    EMPTY = 0, NAME = 1, TYPE_INT32 = 2, TYPE_INT64 = 3, TYPE_REAL64 = 4, 
    TYPE_STRING = 5, REGEXP = 6, EQUALS = 8, NOT_EQUALS = 9, LESS_THAN = 10,
    LESS_THAN_EQUALS = 11, GREATER_THAN = 12, GREATER_THAN_EQUALS = 13,
    OR = 16, XOR = 17, AND = 18, NOT = 19, UNARY_PLUS = 24, UNARY_MINUS = 25,
    MULTIPLY = 26, DIVIDE = 27, MODULO = 28, ADD = 29, SUBTRACT = 30 ,
    SHIFT_LEFT = 32, SHIFT_RIGHT = 33, LOGICAL_SHIFT_RIGHT = 34, 
    BIT_AND = 35, BIT_XOR = 36, BIT_OR = 37, BIT_NEGATE = 38, INT32 = 40, 
    INT64 = 41, REAL64 = 42, STRING = 43, OPAQUE = 44, NAN = 45,
    BEGINS_WITH = 48, CONTAINS = 49, ENDS_WITH = 50, WILDCARD = 51, REGEX = 52,
    TO_LOWER = 56, TO_UPPER = 57, PRIMARY = 58, SECONDARY = 59, TERTIARY = 60,
    DECOMPOSE = 61, DECOMPOSE_COMPAT = 62, REQUIRE = 64, F_EQUALS = 65,
    SIZE = 66;
  
  private static Map<Class<? extends Node>, Integer> NODE_TO_TYPECODE;
  private static Map<Integer, Class<? extends Node>> TYPECODE_TO_NODE;
  
  static
  {
    NODE_TO_TYPECODE = new HashMap<Class<? extends Node>, Integer> ();
    TYPECODE_TO_NODE = new HashMap<Integer, Class<? extends Node>> ();
    
    mapTypeCode (And.class, AND);
    mapTypeCode (Field.class, NAME);
    mapTypeCode (MathBitAnd.class, BIT_AND);
    mapTypeCode (MathBitInvert.class, BIT_NEGATE);
    mapTypeCode (MathBitLogShiftRight.class, LOGICAL_SHIFT_RIGHT);
    mapTypeCode (MathBitOr.class, OR);
    mapTypeCode (MathBitShiftLeft.class, SHIFT_LEFT);
    mapTypeCode (MathBitShiftRight.class, SHIFT_RIGHT);
    mapTypeCode (MathBitXor.class, XOR);
    mapTypeCode (MathDiv.class, DIVIDE);
    mapTypeCode (MathMinus.class, SUBTRACT);
    mapTypeCode (MathMod.class, MODULO);
    mapTypeCode (MathMult.class, MULTIPLY);
    mapTypeCode (MathPlus.class, ADD);
    mapTypeCode (MathUnaryMinus.class, UNARY_MINUS);
    mapTypeCode (Nan.class, NAN);
    mapTypeCode (Not.class, NOT);
    mapTypeCode (Or.class, OR);
    mapTypeCode (Require.class, REQUIRE);
    mapTypeCode (Size.class, SIZE);
    mapTypeCode (StrBeginsWith.class, BEGINS_WITH);
    mapTypeCode (StrContains.class, CONTAINS);
    mapTypeCode (StrEndsWith.class, ENDS_WITH);
    mapTypeCode (StrFoldCase.class, TO_LOWER);
    mapTypeCode (StrRegex.class, REGEX);
    mapTypeCode (StrWildcard.class, WILDCARD);
    mapTypeCode (Xor.class, XOR);
  }
  
  private AstIO ()
  {
    // zip
  }
  
  private static void mapTypeCode (Class<? extends Node> type, int code)
  {
    Integer typeCode = code; // box once
    
    NODE_TO_TYPECODE.put (type, typeCode);
    TYPECODE_TO_NODE.put (typeCode, type);
  }
  
  public static void encodeAST (ByteBuffer out, Node node)
  {
    if (node instanceof Const)
    {
      encodeValue (out, (Const)node);
    } else
    {
      out.putInt (typeCodeFor (node));
      out.putInt (0); // composite node base type
      
      Collection<? extends Node> children = node.children ();
      
      out.putInt (children.size ());
      
      for (Node child : children)
        encodeAST (out, child);
    }
  }

  /**
   * Generate the AST type code for a node, taking into account cases
   * where there is not a 1-1 mapping from Node -> AST node type.
   */
  private static int typeCodeFor (Node node)
  {
    if (node instanceof Compare)
    {
      Compare compare = (Compare)node;
      
      switch (compare.inequality)
      {
        case 0:
          return EQUALS;
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
      // this will NPE on unmapped type
      return NODE_TO_TYPECODE.get (node.getClass ());
    }
  }

  /**
   * Encode a constant value (leaf) node.
   */
  private static void encodeValue (ByteBuffer out, Const node)
  {
    Object value = node.value ();
    Class<?> type = value.getClass ();

    if (type == String.class)
    {
      out.putInt (TYPE_STRING);
      out.putInt (IO.TYPE_STRING);
      putString (out, (String)value);
    } else if (type == Integer.class)
    {
      out.putInt (TYPE_INT32);
      out.putInt (IO.TYPE_INT32);
      out.putInt ((Integer)value);
    } else if (type == Long.class)
    {
      out.putInt (TYPE_INT64);
      out.putInt (IO.TYPE_INT64);
      out.putLong ((Long)value);
    } else if (type == Double.class)
    {
      out.putInt (TYPE_REAL64);
      out.putInt (IO.TYPE_REAL64);
      out.putDouble ((Double)value);
    } else
    {
      throw new Error ();
    }
  }
}
