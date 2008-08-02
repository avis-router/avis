package org.avis.util;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JUTestStreams
{
  @Test
  public void readLine ()
    throws Exception
  {
    List<String> lines = readLines ("line 0\nline 1\r\nline 2\n\rline 3");
    
    assertEquals (4, lines.size ());
    
    for (int i = 0; i < lines.size (); i++)
      assertEquals ("line " + i, lines.get (i));
    
    lines = readLines ("line 0");
    
    assertEquals (1, lines.size ());
    
    for (int i = 0; i < lines.size (); i++)
      assertEquals ("line " + i, lines.get (i));
    
    lines = readLines ("\nline 0\r\n");
    
    assertEquals (2, lines.size ());
    
    assertEquals ("", lines.get (0));
    assertEquals ("line 0", lines.get (1));
    
    lines = readLines ("\n\nline 2\r\n\n\r\r\nline 5\r\r");
    
    assertEquals (7, lines.size ());
    
    assertEquals ("", lines.get (0));
    assertEquals ("", lines.get (1));
    assertEquals ("line 2", lines.get (2));
    assertEquals ("", lines.get (3));
    assertEquals ("", lines.get (4));
    assertEquals ("line 5", lines.get (5));
    assertEquals ("", lines.get (6));
  }

  private static List<String> readLines (String source)
    throws IOException
  {
    List<String> lines = new ArrayList<String> ();
    StringReader in = new StringReader (source);
    
    String line;
    
    while ((line = Streams.readLine (in)) != null)
      lines.add (line);
    
    return lines;
  }
}
