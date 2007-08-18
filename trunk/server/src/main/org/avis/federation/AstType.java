package org.avis.federation;

/**
 * Enumeration of Elvin AST node type codes. See client spec section
 * 8.2.3.4.
 * 
 * @author Matthew Phillips
 */
final class AstType
{
  public static final int
    EMPTY = 0, NAME = 1, CONST_INT32 = 2, CONST_INT64 = 3, CONST_REAL64 = 4, 
    CONST_STRING = 5, REGEXP = 6, EQUALS = 8, NOT_EQUALS = 9, LESS_THAN = 10,
    LESS_THAN_EQUALS = 11, GREATER_THAN = 12, GREATER_THAN_EQUALS = 13,
    OR = 16, XOR = 17, AND = 18, NOT = 19, UNARY_PLUS = 24, UNARY_MINUS = 25,
    MULTIPLY = 26, DIVIDE = 27, MODULO = 28, ADD = 29, SUBTRACT = 30 ,
    SHIFT_LEFT = 32, SHIFT_RIGHT = 33, LOGICAL_SHIFT_RIGHT = 34, 
    BIT_AND = 35, BIT_XOR = 36, BIT_OR = 37, BIT_NEGATE = 38, INT32 = 40, 
    INT64 = 41, REAL64 = 42, STRING = 43, OPAQUE = 44, NAN = 45,
    BEGINS_WITH = 48, CONTAINS = 49, ENDS_WITH = 50, WILDCARD = 51, REGEX = 52,
    FOLD_CASE = 56, TO_UPPER = 57, PRIMARY = 58, SECONDARY = 59, TERTIARY = 60,
    DECOMPOSE = 61, DECOMPOSE_COMPAT = 62, REQUIRE = 64, F_EQUALS = 65,
    SIZE = 66;
}
