package org.avis.federation;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.federation.io.AstXdrCoding;
import org.avis.subscription.ast.Node;

import org.junit.Test;

import static org.avis.subscription.ast.Nodes.unparse;
import static org.avis.subscription.ast.nodes.Const.CONST_FALSE;
import static org.avis.subscription.ast.nodes.Const.CONST_TRUE;
import static org.avis.subscription.parser.SubscriptionParserBase.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JUTestAstXdrCoding
{
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
    
    // check CONST_FALSE gets turned into EMPTY node
    roundtrip (CONST_FALSE);

    // can't do CONST_TRUE
    try
    {
      roundtrip (CONST_TRUE);
      
      fail ();
    } catch (ProtocolCodecException ex)
    {
      // ok
    }
  }
  
  private static void roundtrip (String expr)
    throws Exception
  {
    roundtrip (parse (expr));
  }
  
  private static void roundtrip (Node ast)
    throws Exception
  {
    ByteBuffer in = ByteBuffer.allocate (1024);
    
    AstXdrCoding.encodeAST (in, ast);
    
    in.flip ();
    
    Node copy = AstXdrCoding.decodeAST (in);
    
    assertEquals (0, in.remaining ());
    
    assertEquals (unparse (ast), unparse (copy));
  }
}
