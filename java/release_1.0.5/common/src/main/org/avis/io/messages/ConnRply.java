package org.avis.io.messages;

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import static org.avis.io.XdrCoding.getNameValues;
import static org.avis.io.XdrCoding.putNameValues;

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
    
    putNameValues (out, options);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    options = getNameValues (in);
  }
}
