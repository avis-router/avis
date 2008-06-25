package org.avis.io.messages;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

public class ConfConn extends Message
{
  public static final int ID = 64;
  
  public static final ConfConn INSTANCE = new ConfConn ();
  
  private ConfConn ()
  {
    // zip
  }
  
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
