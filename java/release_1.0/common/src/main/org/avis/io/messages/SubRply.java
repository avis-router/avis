package org.avis.io.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

public class SubRply extends XidMessage
{
  public static final int ID = 61;
  
  public long subscriptionId;
  
  public SubRply ()
  {
    // zip
  }
  
  public SubRply (XidMessage inReplyTo, long subscriptionId)
  {
    super (inReplyTo);
  
    this.subscriptionId = subscriptionId;
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
    
    out.putLong (subscriptionId);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    subscriptionId = in.getLong ();
  }
}
