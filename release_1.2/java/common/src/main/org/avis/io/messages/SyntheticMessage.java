package org.avis.io.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

public abstract class SyntheticMessage extends Message
{
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    throw new ProtocolCodecException ("Synthetic message");
  }

  @Override
  public void encode (ByteBuffer out) 
    throws ProtocolCodecException
  {
    throw new ProtocolCodecException ("Synthetic message");
  }
}
