package org.avis.io.messages;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

public class SubDelRqst extends RequestMessage<SubRply>
{
  public static final int ID = 60;
  
  public long subscriptionId;
  
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
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public Class<SubRply> replyType ()
  {
    return SubRply.class;
  }
  
  @Override
  public void encode (IoBuffer out)
    throws ProtocolCodecException
  {
    super.encode (out);
    
    out.putLong (subscriptionId);
  }
  
  @Override
  public void decode (IoBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    subscriptionId = in.getLong ();
  }
}
