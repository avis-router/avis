package org.avis.util;

import org.junit.Test;

import static org.avis.util.Text.bytesToHex;

import static org.junit.Assert.assertEquals;

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
}
