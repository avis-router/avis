package org.avis.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * A buffered stream reader that hacks around lack of EOF flag on
 * standard Reader's and avoids blocking on EOF test. Only readLine ()
 * is fixed in this version.
 * 
 * @author Matthew Phillips
 */
public class StreamReader extends BufferedReader
{
  private boolean eof;

  public StreamReader (Reader in)
  {
    super (in, 1);
  }

  public StreamReader (InputStream in) 
    throws IOException
  {
    this (new InputStreamReader (in, "UTF-8"));
  }

  @Override
  public String readLine ()
    throws IOException
  {
    if (eof)
    {
      return null;
    } else
    {
      String line = super.readLine ();
      
      if (line == null)
        eof = true;
      
      return line;
    }
  }

  /**
   * Test if EOF has been reached. Guaranteed not to block.
   */
  public boolean eof ()
  {
    return eof;
  }
}
