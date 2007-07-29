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
  public void astIO () 
    throws Exception
  {
    roundtrip ("require (foobar)");
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
