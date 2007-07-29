package org.avis.federation;

import org.apache.mina.common.ByteBuffer;

import org.avis.subscription.ast.Node;

import org.junit.Test;

import static org.avis.subscription.ast.Nodes.unparse;
import static org.avis.subscription.parser.SubscriptionParserBase.parse;

import static org.junit.Assert.assertEquals;

public class JUTestAstXdrCoding
{
  @Test
  public void typeCodes () 
    throws Exception
  {
    assertEquals (2, AstType.CONST_INT32.ordinal ());
    assertEquals (66, AstType.SIZE.ordinal ());
  }
  
  @Test
  public void astIO () 
    throws Exception
  {
    roundtrip ("require (foobar)");
    roundtrip ("foobar == 2");
    roundtrip ("int64 (foobar)");
    roundtrip ("foobar != 'hello'");
    roundtrip ("~foobar << 2 == 6L");
    roundtrip ("! (foobar <= 3.14)");
    roundtrip ("foobar == 'hello' || " +
    	       "greebo & 1 == 1 && begins-with (greebo, 'frob', 'wibble')");
    roundtrip ("size (foobar) > 10 && (foobar - 20 >= 100)");
  }
  
  private static void roundtrip (String expr)
    throws Exception
  {
    Node ast = parse (expr);
    
    ByteBuffer in = ByteBuffer.allocate (1024);
    
    AstXdrCoding.encodeAST (in, ast);
    
    in.flip ();
    
    Node copy = AstXdrCoding.decodeAST (in);
    
    assertEquals (0, in.remaining ());
    
    assertEquals (unparse (ast), unparse (copy));
  }
}
