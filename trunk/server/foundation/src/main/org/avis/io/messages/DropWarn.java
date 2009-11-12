package org.avis.io.messages;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

public class DropWarn extends Message
{
  public static final int ID = 62;
  
  @Override
  public int typeId ()
  {
    return ID;
  }

  @Override
  public void decode (IoBuffer in)
    throws ProtocolCodecException
  {
    // zip
  }

  @Override
  public void encode (IoBuffer out)
    throws ProtocolCodecException
  {
    // zip
  }
}
