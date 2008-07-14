package org.avis.federation;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.federation.io.XdrAstCoding;
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
    
    roundtrip ("regex (name, 'a.*b')");
    roundtrip ("name == 1 || name != 8 || name < 3");
    roundtrip ("name == 1 || name != 8 ^^ ! (name >= 3)");
    roundtrip ("name == name + 3 / 4 % foobar >>> 6");
    roundtrip ("name == x & 1 | y");
    roundtrip ("name == x ^ 1 | -y");
    roundtrip ("int32 (name1) || string (name2)");
    
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
    IoBuffer in = IoBuffer.allocate (1024);
    
    XdrAstCoding.encodeAST (in, ast);
    
    in.flip ();
    
    Node copy = XdrAstCoding.decodeAST (in);
    
    assertEquals (0, in.remaining ());
    
    assertEquals (unparse (ast), unparse (copy));
  }
}
