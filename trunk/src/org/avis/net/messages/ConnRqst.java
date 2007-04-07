package org.avis.net.messages;

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.net.security.Keys;

import static java.util.Collections.emptyMap;

import static org.avis.net.common.IO.getNameValues;
import static org.avis.net.common.IO.putNameValues;
import static org.avis.net.security.Keys.EMPTY_KEYS;

/**
 * 
 * @author Matthew Phillips
 */
public class ConnRqst extends XidMessage
{
  public static final Map<String, Object> EMPTY_OPTIONS = emptyMap ();

  public static final int ID = 49;
  
  public int versionMajor;
  public int versionMinor;
  public Map<String, Object> options;
  public Keys notificationKeys;
  public Keys subscriptionKeys;

  public ConnRqst ()
  {
    // zip
  }
  
  public ConnRqst (int major, int minor)
  {
    this (major, minor, EMPTY_OPTIONS);
  }

  public ConnRqst (int major, int minor, Map<String, Object> options)
  {
    super (nextXid ());
    
    this.versionMajor = major;
    this.versionMinor = minor;
    this.options = options;
    this.notificationKeys = EMPTY_KEYS;
    this.subscriptionKeys = EMPTY_KEYS;
  }
  
  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    super.encode (out);
    
    out.putInt (versionMajor);
    out.putInt (versionMinor);
    
    putNameValues (out, options);

    notificationKeys.encode (out);
    subscriptionKeys.encode (out);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    versionMajor = in.getInt ();
    versionMinor = in.getInt ();
    
    options = getNameValues (in);
    
    notificationKeys = Keys.decode (in);
    subscriptionKeys = Keys.decode (in);
  }
}
