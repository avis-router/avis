package org.avis.net.messages;

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.net.security.Keys;

import static org.avis.net.security.Keys.EMPTY_KEYS;

public class UNotify extends Notify
{
  public static final int ID = 32;
  
  public int clientMajorVersion;
  public int clientMinorVersion;
  
  public UNotify ()
  {
    // zip
  }
  
  public UNotify (int clientMajorVersion, 
                  int clientMinorVersion,
                  Map<String, Object> attributes)
  {
    this (clientMajorVersion, clientMinorVersion, attributes,
          true, EMPTY_KEYS);
  }
  
  public UNotify (int clientMajorVersion, 
                  int clientMinorVersion,
                  Map<String, Object> attributes,
                  boolean deliverInsecure,
                  Keys keys)
  {
    super (attributes, deliverInsecure, keys);
    
    this.clientMajorVersion = clientMajorVersion;
    this.clientMinorVersion = clientMinorVersion;
  }
  
  @Override
  public int typeId ()
  {
    return ID;
  }

  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    clientMajorVersion = in.getInt ();
    clientMinorVersion = in.getInt ();
    
    super.decode (in);
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    out.putInt (clientMajorVersion);
    out.putInt (clientMinorVersion);
    
    super.encode (out);
  }
}
