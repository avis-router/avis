package org.avis.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JUTestNotification
{
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
      ("real64: 3.14\n" + 
       "int32: 42\n" + 
       "string: \"hello \\\"world\\\"\"\n" + 
       "real64\\ #2: 3.0\n" + 
       "opaque: [01 02 03 de ad be ef]\n" + 
       "int64: 4242L\n" + 
       "field\\ with\\ tricky:characters: \"bad\"", toString);
  }
}
