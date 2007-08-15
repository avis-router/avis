package org.avis.io;

import java.util.HashMap;
import java.util.Map;

import java.lang.reflect.Array;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.IO;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the {@link IO} utility class.
 */
public class JUTestIO
{
  @Test 
  public void stringIO ()
    throws Exception
  {
    ByteBuffer buff = ByteBuffer.allocate (1024);
    
    IO.putString (buff, "");
    
    assertEquals (4, buff.position ());
    
    buff.position (0);
    
    IO.putString (buff, "hello");
    
    assertEquals (12, buff.position ());
    
    buff.position (0);
    assertEquals ("hello", IO.getString (buff));
  }
  
  @Test
  public void utf8 ()
    throws Exception
  {
    roundtrip ("Hello A\u0308\uFB03ns this is some bogus text");
    roundtrip ("Hi there \u00C4\uFB03ns more bogus text");
    
    // some UTF-8 data seen in the wild that caused problems...
    roundtrip
      (new String 
        (new byte [] {(byte)0xc3, (byte)0x94, (byte)0xc3, (byte)0xb8, 
                      (byte)0xce, (byte)0xa9, 0x73, 0x20, 0x73, 0x75, 0x70,
                      0x70, 0x6f, 0x73, 0x65, 0x20}, "UTF-8"));
  }

  private void roundtrip (String str)
    throws ProtocolCodecException
  {
    ByteBuffer buff = ByteBuffer.allocate (1024);
    
    IO.putString (buff, str);
    
    buff.flip ();
    
    assertEquals (str, IO.getString (buff));
  }
  
  @Test 
  public void nameValueIO ()
    throws Exception
  {
    ByteBuffer buff = ByteBuffer.allocate (1024);
    HashMap<String, Object> nameValues = new HashMap<String, Object> ();
    
    IO.putNameValues (buff, nameValues);
    assertEquals (4, buff.position ());
    
    buff.position (0);
    nameValues.put ("int", 42);
    nameValues.put ("opaque", new byte [] {1, 2, 3});
    
    IO.putNameValues (buff, nameValues);
    assertEquals (44, buff.position ());
    
    buff.flip ();
    assertMapsEqual (nameValues, IO.getNameValues (buff));
  }

  @Test 
  public void objectsIO ()
    throws Exception
  {
    ByteBuffer buff = ByteBuffer.allocate (1024);
    Object [] objects = new Object [] {"hello", Integer.valueOf (42)};
    
    IO.putObjects (buff, objects);
    
    buff.flip ();
    
    Object [] objectsCopy = IO.getObjects (buff);
    
    assertArraysEquals (objects, objectsCopy);
  }
  
  @Test
  public void padding ()
  {
    assertEquals (0, IO.paddingFor (0));
    assertEquals (3, IO.paddingFor (1));
    assertEquals (2, IO.paddingFor (2));
    assertEquals (1, IO.paddingFor (3));
    assertEquals (0, IO.paddingFor (4));
    assertEquals (3, IO.paddingFor (5));
    assertEquals (3, IO.paddingFor (25));
    assertEquals (3, IO.paddingFor (4 * 1234 + 1));
  }
  
  private void assertArraysEquals (Object [] o1, Object [] o2)
  {
    assertEquals (o1.length, o2.length);
    
    for (int i = 0; i < o1.length; i++)
      assertEquals (o1 [i], o2 [i]);
  }

  private void assertMapsEqual (Map<String, Object> map1,
                                Map<String, Object> map2)
  {
    assertEquals (map1.size (), map2.size ());
    
    for (String name : map1.keySet ())
      assertTrue (objectsEqual (map1.get (name), map2.get (name)));
    
    for (String name : map2.keySet ())
      assertTrue (objectsEqual (map1.get (name), map2.get (name)));
  }
  
  private static boolean objectsEqual (Object o1, Object o2)
  {
    if (o1 == o2 || (o1 != null && o1.equals (o2)))
    {
      return true;
    } else if (o1 != null && o2 != null)
    {
      // deep test for array equality
      Class<?> cls = o1.getClass ();
      
      if (cls == o2.getClass () && cls.isArray ())
      {
        int length = Array.getLength (o1);
        
        if (length == Array.getLength (o2))
        {
          for (int i = 0; i < length; i++)
          {
            if (!objectsEqual (Array.get (o1, i), Array.get (o2, i)))
              return false;
          }
          
          return true;
        }
      }      
    }
    
    return false;
  }
}
