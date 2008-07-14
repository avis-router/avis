package org.avis.io.messages;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

public abstract class SyntheticMessage extends Message
{
  @Override
  public void decode (IoBuffer in)
    throws ProtocolCodecException
  {
    throw new ProtocolCodecException ("Synthetic message");
  }

  @Override
  public void encode (IoBuffer out) 
    throws ProtocolCodecException
  {
    throw new ProtocolCodecException ("Synthetic message");
  }
}
