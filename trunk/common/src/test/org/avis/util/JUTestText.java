package org.avis.util;

import org.junit.Test;

import static org.avis.util.Text.bytesToHex;
import static org.avis.util.Text.expandBackslashes;
import static org.avis.util.Text.stripBackslashes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JUTestText
{
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
}
