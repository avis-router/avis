package org.avis.net.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

public class SubDelRqst extends XidMessage
{
  public static final int ID = 60;
  
  public long subscriptionId;

  @Override
  public int typeId ()
  {
    return ID;
  }
  
  public SubDelRqst ()
  {
    // zip
  }
  
  public SubDelRqst (long subscriptionId)
  {
    super (nextXid ());
    
    this.subscriptionId = subscriptionId;
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
