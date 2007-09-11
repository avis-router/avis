package org.avis.net.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolViolationException;

public class ConfConn extends Message
{
  public static final int ID = 64;
  
  @Override
  public int typeId ()
  {
    return ID;
  }

  @Override
  public void decode (ByteBuffer in)
    throws ProtocolViolationException
  {
    // zip
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolViolationException
  {
    // zip
  }
}
