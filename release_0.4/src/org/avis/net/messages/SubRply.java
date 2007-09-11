package org.avis.net.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolViolationException;

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
    throws ProtocolViolationException
  {
    super.encode (out);
    
    out.putLong (subscriptionId);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolViolationException
  {
    super.decode (in);
    
    subscriptionId = in.getLong ();
  }
}
