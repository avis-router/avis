package org.avis.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import org.avis.util.InvalidFormatException;

import static java.util.Arrays.asList;

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
    
    ntfn1.set ("string", "hello \"world\"");
    ntfn1.set ("int32", 42);
    ntfn1.set ("int64", 4242L);
    ntfn1.set ("real64", 3.14D);
    ntfn1.set ("real64 #2", 3D);
    ntfn1.set ("field with tricky:characters", "bad");
    ntfn1.set ("opaque",
               new byte [] {1, 2, 3, (byte)0xDE, (byte)0xAD,
                            (byte)0xBE, (byte)0xEF});
    
    Notification ntfn2 = new Notification ();
    
    ntfn2.set ("real64 #2", 3D);
    ntfn2.set ("string", "hello \"world\"");
    ntfn2.set ("int32", 42);
    ntfn2.set ("real64", 3.14D);
    ntfn2.set ("int64", 4242L);
    ntfn2.set ("field with tricky:characters", "bad");
    ntfn2.set ("opaque",
               new byte [] {1, 2, 3,
                            (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF});
    
    // ntfn3 is slightly different
    Notification ntfn3 = new Notification ();
    
    ntfn3.set ("real64 #2", 3D);
    ntfn3.set ("string", "hello \"world\"");
    ntfn3.set ("int32", 42);
    ntfn3.set ("real64", 3.14D);
    ntfn3.set ("int64", 4242L);
    ntfn3.set ("field with tricky:characters", "bad");
    ntfn3.set ("opaque",
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
    
    ntfn.set ("string", "hello \"world\"");
    ntfn.set ("int32", 42);
    ntfn.set ("int64", 4242L);
    ntfn.set ("real64", 3.14D);
    ntfn.set ("real64 #2", 3D);
    ntfn.set ("field with tricky:characters", "bad");
    ntfn.set ("opaque",
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
  public void readLine ()
    throws Exception
  {
    String basic = "test: 1\ntest: 2";
    assertEquals (asList ("test: 1", "test: 2"), readLines (basic));
    
    String quotes = "test: \"1\"\ntest: \"2\"";
    assertEquals (asList ("test: \"1\"", "test: \"2\""), readLines (quotes));
    
    String quotesWithNl = "test: \"1\n2\n 3\"\ntest: \"2\"";
    assertEquals (asList ("test: \"12 3\"", "test: \"2\""), 
                  readLines (quotesWithNl));
    String dataWithNl = "test: [de ad\n\n be ef]\ntest: 2";
    assertEquals (asList ("test: [de ad be ef]", "test: 2"), 
                  readLines (dataWithNl));
    
    String continuation = "test: \\\n1\ntest: 2";
    assertEquals (asList ("test: 1", "test: 2"), readLines (continuation));
  }
  
  private static List<String> readLines (String content) 
    throws IOException
  {
    ArrayList<String> lines = new ArrayList<String> ();
    
    BufferedReader reader = new BufferedReader (new StringReader (content));
    String line;
    
    while ((line = Notification.nextLine (reader)) != null)
      lines.add (line);
    
    return lines;
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
    
    ntfn.set ("string", "hello \"world\"");
    ntfn.set ("int32", 42);
    ntfn.set ("int64", 4242L);
    ntfn.set ("real64", 3.14D);
    ntfn.set ("real64 #2", 3D);
    ntfn.set ("field with tricky:characters", "bad");
    ntfn.set ("opaque",
              new byte [] {1, 2, 3,
                           (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF});
    
    Notification rountrippedNtfn = new Notification (ntfn.toString ());
    
    assertEquals (ntfn, rountrippedNtfn);
  }

  @Test
  public void typeSafety ()
  {
    Notification ntfn = new Notification ();
    
    assertCannotSet (ntfn, "field", new int [] {});
    assertCannotSet (ntfn, "field", 3.14f);
    
    ntfn.set ("field", 42);
    
    try
    {
      ntfn.getString ("field");
      
      fail ();
    } catch (IllegalArgumentException ex)
    {
      // ok
    }
  }
  
  @Test
  public void testConstructors ()
  {
    Map<String, Object> map = new HashMap<String, Object> ();
    
    map.put ("int", 32);
    map.put ("string", "hello");
    map.put ("long", 42L);
    
    Notification ntfn = new Notification (map);
    assertEquals (map, ntfn.asMap ());
    
    ntfn = new Notification ("int", 32, "string", "hello", "long", 42L);
    assertEquals (map, ntfn.asMap ());
    
    // check bad values
    map.put ("bad", new Date ());
    
    try
    {
      new Notification (map);
      
      fail ();
    } catch (IllegalArgumentException ex)
    {
      // ok
    }
  }
  
  private static void assertCannotSet (Notification ntfn,
                                       String field, Object value)
  {
    try
    {
      ntfn.set (field, value);
      
      fail ("Failed to detect illegal value: " + value);
    } catch (IllegalArgumentException ex)
    {
      // System.out.println ("message = " + ex.getMessage ());
      // ok
    }
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
