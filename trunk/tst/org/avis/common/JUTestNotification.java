package org.avis.common;

import java.util.Arrays;

import org.avis.util.InvalidFormatException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JUTestNotification
{
  @Test
  public void equality ()
  {
    Notification ntfn1 = new Notification ();
    
    ntfn1.put ("string", "hello \"world\"");
    ntfn1.put ("int32", 42);
    ntfn1.put ("int64", 4242L);
    ntfn1.put ("real64", 3.14D);
    ntfn1.put ("real64 #2", 3D);
    ntfn1.put ("field with tricky:characters", "bad");
    ntfn1.put ("opaque",
              new byte [] {1, 2, 3,
                           (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF});
    
    Notification ntfn2 = new Notification ();
    
    ntfn2.put ("real64 #2", 3D);
    ntfn2.put ("string", "hello \"world\"");
    ntfn2.put ("int32", 42);
    ntfn2.put ("real64", 3.14D);
    ntfn2.put ("int64", 4242L);
    ntfn2.put ("field with tricky:characters", "bad");
    ntfn2.put ("opaque",
              new byte [] {1, 2, 3,
                           (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF});
    
    // ntfn3 is slightly different
    Notification ntfn3 = new Notification ();
    
    ntfn3.put ("real64 #2", 3D);
    ntfn3.put ("string", "hello \"world\"");
    ntfn3.put ("int32", 42);
    ntfn3.put ("real64", 3.14D);
    ntfn3.put ("int64", 4242L);
    ntfn3.put ("field with tricky:characters", "bad");
    ntfn3.put ("opaque",
              new byte [] {1, 2, 3,
                           (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEE});
    
    assertEquals (ntfn1, ntfn2);
    assertEquals (ntfn1.hashCode (), ntfn2.hashCode ());
    
    assertFalse (ntfn1.equals (ntfn3));
  }
  
  @Test
  public void stringFormat ()
  {
    Notification ntfn = new Notification ();
    
    ntfn.put ("string", "hello \"world\"");
    ntfn.put ("int32", 42);
    ntfn.put ("int64", 4242L);
    ntfn.put ("real64", 3.14D);
    ntfn.put ("real64 #2", 3D);
    ntfn.put ("field with tricky:characters", "bad");
    ntfn.put ("opaque",
              new byte [] {1, 2, 3,
                           (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF});
    
    String toString = ntfn.toString ();
    
    // System.out.println ("toString () = \n" + toString);
    
    assertEquals
      ("field\\ with\\ tricky\\:characters: \"bad\"\n" + 
       "int32: 42\n" + 
       "int64: 4242L\n" + 
       "opaque: [01 02 03 de ad be ef]\n" + 
       "real64: 3.14\n" + 
       "real64\\ #2: 3.0\n" + 
       "string: \"hello \\\"world\\\"\"", toString);
  }
  
  @Test
  public void parseSimple ()
    throws Exception
  {
    String ntfnStr =
      "String: \"string\"\n" +
      "Int32: 42\n" +
      "Int64: 24L\n" +
      "Real64: 3.1415\n" +
      "Opaque: [01 02 0a 0b 0c 1a ab ff]";
    
    Notification ntfn = new Notification (ntfnStr);
    
    assertEquals ("string", ntfn.get ("String"));
    assertEquals (42, ntfn.get ("Int32"));
    assertEquals (24L, ntfn.get ("Int64"));
    assertEquals (3.1415, ntfn.get ("Real64"));
    assertTrue
      ("Opaque arrays equal",
       Arrays.equals
        (new byte [] {0x01, 0x02, 0x0a, 0x0b, 0x0c, 0x1a, (byte)0xab, (byte)0xff},
         ntfn.getOpaque ("Opaque")));
  }
  
  @Test
  public void parseTricky ()
    throws Exception
  {
    String ntfnStr =
      "Str\\ ing: \"str\\\"ing\"\n" +
      "Int\\:32: 42\n" +
      "Int64:24L\n" +
      "Real64 : 3.1415\n" +
      "Opaque:[ ]";
    
    Notification ntfn = new Notification (ntfnStr);
    
    assertEquals ("str\"ing", ntfn.get ("Str ing"));
    assertEquals (42, ntfn.get ("Int:32"));
    assertEquals (24L, ntfn.get ("Int64"));
    assertEquals (3.1415, ntfn.get ("Real64"));
    assertTrue
      ("Opaque arrays equal",
       Arrays.equals
        (new byte [] {},
         ntfn.getOpaque ("Opaque")));
  }
  
  @Test
  public void parseErrors ()
    throws Exception
  {
    assertInvalid ("Hello");
    assertInvalid ("hello: ");
    assertInvalid ("he:llo: 1");
    assertInvalid ("hello: 1.a");
    assertInvalid ("hello: hello");
    assertInvalid ("hello: \"hello");
    assertInvalid ("hello: \"hello\"\"");
    assertInvalid ("hello: [");
    assertInvalid ("hello: []]");
    assertInvalid ("hello: [01 ");
    assertInvalid ("hello: [01 001]");
    assertInvalid ("hello: [01 1h]");
    assertInvalid ("hello: [01 1h]");
    assertInvalid ("hello: 1234567890123");
  }
  
  @Test
  public void roundtrip ()
    throws Exception
  {
    Notification ntfn = new Notification ();
    
    ntfn.put ("string", "hello \"world\"");
    ntfn.put ("int32", 42);
    ntfn.put ("int64", 4242L);
    ntfn.put ("real64", 3.14D);
    ntfn.put ("real64 #2", 3D);
    ntfn.put ("field with tricky:characters", "bad");
    ntfn.put ("opaque",
              new byte [] {1, 2, 3,
                           (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF});
    
    Notification rountrippedNtfn = new Notification (ntfn.toString ());
    
    assertEquals (ntfn, rountrippedNtfn);
  }

  private static void assertInvalid (String ntfnExpr)
  {
    try
    {
      new Notification (ntfnExpr);
      
      fail ("Failed to detect invalid expression: " + ntfnExpr);
    } catch (InvalidFormatException ex)
    {
      // System.out.println (ex.getMessage ());
      // ok
    }
  }
}
