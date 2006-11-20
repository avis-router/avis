package org.avis.net;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import java.nio.BufferUnderflowException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import static java.nio.CharBuffer.wrap;
import static java.util.Collections.emptyMap;

/**
 * I/O helpers for the Elvin XDR wire format.
 * 
 * @author Matthew Phillips
 */
public final class IO
{
  /**
   * Type codes from client protocol spec.
   *  enum {
   *       int32_tc  = 1,
   *       int64_tc  = 2,
   *       real64_tc = 3,
   *       string_tc = 4,
   *       opaque_tc = 5
   *   } value_typecode;
   */
  private static final int TYPE_INT32  = 1;
  private static final int TYPE_INT64  = 2;
  private static final int TYPE_REAL64 = 3;
  private static final int TYPE_STRING = 4;
  private static final int TYPE_OPAQUE = 5;

  private static final byte [] EMPTY_BYTES = new byte [0];
  
  /**
   * Per thread UTF-8 decoder.
   */
  private static final ThreadLocal<CharsetDecoder> UTF8_DECODER =
    new ThreadLocal<CharsetDecoder> ()
  {
    protected CharsetDecoder initialValue ()
    {
      return Charset.forName ("UTF-8").newDecoder ();
    }
  };
  
  /**
   * Per thread UTF-8 encoder.
   */
  private static final ThreadLocal<CharsetEncoder> UTF8_ENCODER =
    new ThreadLocal<CharsetEncoder> ()
  {
    protected CharsetEncoder initialValue ()
    {
      return Charset.forName ("UTF-8").newEncoder ();
    }
  };
 
  private IO ()
  {
    // zip
  }
  
  public static byte [] toUTF8 (String string)
  { 
    try
    {
      if (string.length () == 0)
        return EMPTY_BYTES;
      else
        return UTF8_ENCODER.get ().encode (wrap (string)).array ();
    } catch (CharacterCodingException ex)
    {
      // shouldn't be possible to get an error encoding from UTF-16 to UTF-8.
      throw new Error ("Internal error", ex);
    }
  }
  
  /**
   * Read a length-delimited 4-byte-aligned UTF-8 string.
   */
  public static String getString (ByteBuffer in)
    throws BufferUnderflowException, ProtocolCodecException
  {
    try
    {
      int length = in.getInt ();

      if (length == 0)
      {
        return "";
      } else
      {
        String string = in.getString (length, UTF8_DECODER.get ());
        
        in.skip (paddingFor (length));
        
        return string;
      }
    } catch (CharacterCodingException ex)
    {
      throw new ProtocolCodecException ("Invalid UTF-8 string", ex);
    }
  }

  /**
   * Write a length-delimited 4-byte-aligned UTF-8 string.
   */
  public static void putString (ByteBuffer out, String string)
  {
    try
    {
      if (string.length () == 0)
      {
        out.putInt (0);
      } else
      {
        out.putInt (string.length ());
        out.putString (string, UTF8_ENCODER.get ());
        putPadding (out, string.length ());
      }
    } catch (CharacterCodingException ex)
    {
      // shouldn't be possible to get an error encoding from UTF-16 to UTF-8.
      throw new Error ("Internal error", ex);
    }
  }

  /**
   * Generate null padding to 4-byte pad out a block of a given length
   */
  public static void putPadding (ByteBuffer out, int length)
  {
    putBytes (out, (byte)0, paddingFor (length));
  }

  /**
   * Calculate the padding for a block of length bytes to a multiple
   * of 4.
   * 
   * TODO opt: there's probably some funky faster way to do this.
   */
  public static int paddingFor (int length)
  {
    int mod = length % 4;
    
    return mod == 0 ? 0 : 4 - mod;
  }

  /**
   * Write a name/value set.
   */
  public static void putNameValues (ByteBuffer out,
                                    Map<String, Object> nameValues)
    throws ProtocolCodecException
  {
    out.putInt (nameValues.size ());
    
    for (Entry<String, Object> entry : nameValues.entrySet ())
    {
      putString (out, entry.getKey ());
      putObject (out, entry.getValue ());
    }
  }

  /**
   * Read a name/value set.
   */
  public static Map<String, Object> getNameValues (ByteBuffer in)
    throws ProtocolCodecException
  {
    int pairs = in.getInt ();
    
    if (pairs == 0)
      return emptyMap ();
    
    HashMap<String, Object> nameValues = new HashMap<String, Object> ();
    
    for ( ; pairs > 0; pairs--)
      nameValues.put (getString (in), getObject (in));

    return nameValues;
  }

  public static void putObjects (ByteBuffer out, Object [] objects)
    throws ProtocolCodecException
  {
    out.putInt (objects.length);
    
    for (Object object : objects)
      putObject (out, object);
  }
  
  public static Object [] getObjects (ByteBuffer in)
    throws ProtocolCodecException
  {
    Object [] objects = new Object [in.getInt ()];
    
    for (int i = 0; i < objects.length; i++)
      objects [i] = getObject (in);
    
    return objects;
  }
  
  /**
   * Put an object value in type_id/value format.
   */
  public static void putObject (ByteBuffer out, Object value)
    throws ProtocolCodecException
  {
    if (value instanceof String)
    {
      out.putInt (TYPE_STRING);
      putString (out, (String)value);
    } else if (value instanceof Integer)
    {
      out.putInt (TYPE_INT32);
      out.putInt ((Integer)value);
    } else if (value instanceof Long)
    {
      out.putInt (TYPE_INT64);
      out.putLong ((Long)value);
    } else if (value instanceof Double)
    {
      out.putInt (TYPE_REAL64);
      out.putDouble ((Double)value);
    } else if (value instanceof byte [])
    {
      out.putInt (TYPE_OPAQUE);
      putBytes (out, (byte [])value);
    } else if (value == null)
    {
      throw new IllegalArgumentException ("Value cannot be null");
    } else
    {
      throw new IllegalArgumentException
        ("Don't know how to encode " + value.getClass ());
    }
  }
  
  /**
   * Read an object in type_id/value format.
   */
  public static Object getObject (ByteBuffer in)
    throws ProtocolCodecException
  {
    int type = in.getInt ();
    
    switch (type)
    {
      case TYPE_INT32:
        return in.getInt ();
      case TYPE_INT64:
        return in.getLong ();
      case TYPE_REAL64:
        return in.getDouble ();
      case TYPE_STRING:
        return getString (in);
      case TYPE_OPAQUE:
        return getBytes (in);
      default:
        throw new ProtocolCodecException ("Unknown type code: " + type);
    }
  }
  
  /**
   * Write a length-delimited, 4-byte-aligned byte array.
   */
  public static void putBytes (ByteBuffer out, byte [] bytes)
  {
    out.putInt (bytes.length);
    out.put (bytes);
    putPadding (out, bytes.length);
  }

  /**
   * Write a series of bytes.
   * 
   * @param out The buffer to write to.
   * @param value The value to write
   * @param count The number of times to write value.
   */
  public static void putBytes (ByteBuffer out, byte value, int count)
  {
    for ( ; count > 0; count--)
      out.put (value);
  }
  
  /**
   * Read a length-delimited, 4-byte-aligned byte array.
   */
  public static byte [] getBytes (ByteBuffer in)
  {
    return getBytes (in, in.getInt ());
  }
  
  /**
   * Read a length-delimited, 4-byte-aligned byte array with a given length.
   */
  public static byte [] getBytes (ByteBuffer in, int length)
  {
    byte [] bytes = new byte [length];
    
    in.get (bytes);
    in.skip (paddingFor (length));
    
    return bytes;
  }

  public static void putBool (ByteBuffer out, boolean value)
  {
    out.putInt (value ? 1 : 0);
  }
  
  public static boolean getBool (ByteBuffer in)
    throws ProtocolCodecException
  {
    int value = in.getInt ();
    
    if (value == 0)
      return false;
    else if (value == 1)
      return true;
    else
      throw new ProtocolCodecException
        ("Cannot interpret " + value + " as boolean");
  }
  
  /**
   * Read a length-demlimited array of longs.
   */
  public static long [] getLongArray (ByteBuffer in)
  {
    long [] longs = new long [in.getInt ()];
    
    for (int i = 0; i < longs.length; i++)
      longs [i] = in.getLong ();
    
    return longs;
  }

  /**
   * Write a length-delimted array of longs.
   */
  public static void putLongArray (ByteBuffer out, long [] longs)
  {
    out.putInt (longs.length);
    
    for (long l : longs)
      out.putLong (l);
  }
}
