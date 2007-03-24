package org.avis.net.messages;

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.net.common.IO;

public class ConnRply extends XidMessage
{
  public static final int ID = 50;

  /** Options requested by client that are supported. */
  public Map<String, Object> options;
  
  public ConnRply ()
  {
    // zip
  }
  
  public ConnRply (ConnRqst inReplyTo, Map<String, Object> options)
  {
    super (inReplyTo);
    
    this.options = options;
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
    
    IO.putNameValues (out, options);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    options = IO.getNameValues (in);
  }
}
