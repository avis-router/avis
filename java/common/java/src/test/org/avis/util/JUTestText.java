package org.avis.util;

import java.util.Arrays;

import org.junit.Test;

import static org.avis.util.Text.bytesToHex;
import static org.avis.util.Text.dataToBytes;
import static org.avis.util.Text.expandBackslashes;
import static org.avis.util.Text.stripBackslashes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JUTestText
{
  @Test
  public void stringToValue () 
    throws Exception
  {
    assertEquals ("hello", Text.stringToValue ("'hello'"));
    assertEquals ("hello", Text.stringToValue ("\"hello\""));
    assertEquals (32, Text.stringToValue ("32"));
    assertEquals (42L, Text.stringToValue ("42L"));
    assertEquals (0.12, Text.stringToValue ("0.12"));
    assertEquals ("0e ff de", 
                  Text.bytesToHex ((byte [])Text.stringToValue ("[0e ff de]")));
  }
  
  @Test
  public void parseHexBytes ()
     throws Exception
  {
    assertEquals (bytesToHex (new byte [] {00, (byte)0xA1, (byte)0xDE,
                               (byte)0xAD, (byte)0xBE, (byte)0xEF}), 
                  bytesToHex (Text.hexToBytes ("00A1DEADBEEF")));
  }
  
  /**
   * Test handling of backslash escapes in strings and identifiers.
   */
  @Test
  public void escapeHandling ()
    throws Exception
  {
    String expanded;
    
    expanded = expandBackslashes ("\\n\\t\\b\\r\\f\\v\\a");
    assertEquals
      (new String (new byte [] {'\n', '\t', '\b', '\r', '\f', 11, 7}), expanded);
    
    expanded = expandBackslashes ("\\x32");
    assertEquals
      (new String (new byte [] {0x32}), expanded);
    
    expanded = expandBackslashes ("\\xf:");
    assertEquals
      (new String (new byte [] {0xF, ':'}), expanded);
    
    expanded = expandBackslashes ("\\x424");
    assertEquals
      (new String (new byte [] {0x42, '4'}), expanded);
    
    expanded = expandBackslashes ("\\034");
    assertEquals
      (new String (new byte [] {034}), expanded);
    
    expanded = expandBackslashes ("\\34:");
    assertEquals
      (new String (new byte [] {034, ':'}), expanded);
    
    expanded = expandBackslashes ("\\7:");
    assertEquals
      (new String (new byte [] {07, ':'}), expanded);
    
    expanded = expandBackslashes ("\\1234");
    assertEquals
      (new String (new byte [] {0123, '4'}), expanded);
    
    expanded = stripBackslashes ("a \\test\\ string\\:\\\\!");
    assertEquals ("a test string:\\!", expanded);

    try
    {
      expanded = stripBackslashes ("invalid\\");
      
      fail ();
    } catch (InvalidFormatException ex)
    {
      // ok
    }
  }
  
  @Test
  public void dataToByte ()
    throws Exception
  {
    byte [] bytes = dataToBytes ("[00 01 0a ff] ".getBytes ("UTF-8"));
    
    assertTrue (Arrays.equals (new byte [] {00, 01, 0x0a, (byte)0xff}, bytes));
    
    bytes = dataToBytes ("\"hello \u0234 world\"\n".getBytes ("UTF-8"));
    
    assertEquals ("hello \u0234 world", new String (bytes, "UTF-8"));
    
    bytes = dataToBytes (new byte [] {'#', (byte)0xde, (byte)0xad});
    
    assertTrue (Arrays.equals (new byte [] {(byte)0xde, (byte)0xad}, bytes));
  }
}
