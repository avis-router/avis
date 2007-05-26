package org.avis.util;

import java.util.Properties;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * General Avis utility functions.
 * 
 * @author Matthew Phillips
 */
public final class Util
{
  private Util ()
  {
    // zip
  }

  /**
   * Test if two objects are equal, handling null values and type differences. 
   */
  public static boolean valuesEqual (Object value1, Object value2)
  {
    if (value1 == value2)
      return true;
    else if (value1 == null || value2 == null)
      return false;
    else if (value1.getClass () == value2.getClass ())
      return value1.equals (value2);
    else
      return false;
  }

  /**
   * Open an input stream on a file.
   */
  public static InputStream fileStream (String filename)
    throws FileNotFoundException
  {
    return new BufferedInputStream (new FileInputStream (filename));
  }

  /**
   * Open an input stream on a resource.
   */
  public static InputStream resourceStream (String resource)
    throws FileNotFoundException
  {
    InputStream in = Util.class.getResourceAsStream (resource);
    
    if (in == null)
      throw new FileNotFoundException ("Missing resource: " + resource);
    else
      return in;
  }

  /**
   * Load a set of java.util.Properties from an input stream and close it.
   */
  public static Properties propertiesFrom (InputStream in)
    throws IOException
  {
    Properties properties = new Properties ();
    
    try
    {
      properties.load (in);
    } finally
    {
      try
      {
        in.close ();
      } catch (IOException ex)
      {
        // zip
      }
    } 
  
    return properties;
  }

  /**
   * Read the entirety of a UTF-8 encoded input stream into a string.
   */
  public static String stringFrom (InputStream stream)
    throws IOException
  {
    return stringFrom (new InputStreamReader (stream, "UTF-8"));
  }
  
  /**
   * Read the entirety of stream into a string.
   */
  public static String stringFrom (Reader reader)
    throws IOException
  {
    StringBuilder str = new StringBuilder ();
    
    char [] buffer = new char [4096];
    int length;
    
    while ((length = reader.read (buffer)) != -1)
      str.append (buffer, 0, length);
    
    reader.close ();
    
    return str.toString ();
  }

  /**
   * Reader's and InputStream's (almost unbelievably) do not have a
   * way to tell when the stream is at eof without modifying it. This
   * uses mark () and read () to non-destructively test for eof. The
   * stream must support mark ().
   */
  public static boolean eof (Reader in)
    throws IOException
  {
    in.mark (10);
    
    if (in.read () == -1)
    {
      return true;
    } else
    {
      in.reset ();
      
      return false;
    }
  }

  /**
   * Generate a buffered reader wrapper for a reader, if it is not
   * already one.
   */
  public static BufferedReader bufferedReaderFor (Reader reader)
  {
    return reader instanceof BufferedReader ? (BufferedReader)reader :
                                              new BufferedReader (reader);
  }
}
