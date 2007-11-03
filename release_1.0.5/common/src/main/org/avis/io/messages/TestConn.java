package org.avis.io.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

public class TestConn extends Message
{
  public static final int ID = 63;
  
  public static final TestConn INSTANCE = new TestConn ();
  
  private TestConn ()
  {
    // zip
  }
  
  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    // zip
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    // zip
  }
}
