package org.avis.net.messages;

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolViolationException;

import org.avis.net.IO;

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
    throws ProtocolViolationException
  {
    super.encode (out);
    
    IO.putNameValues (out, options);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolViolationException
  {
    super.decode (in);
    
    options = IO.getNameValues (in);
  }
}
