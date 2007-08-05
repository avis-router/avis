package org.avis.io.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

/**
 * Synthetic message used to signal protocol errors.
 * 
 * @author Matthew Phillips
 */
public class ErrorMessage extends Message
{
  public static final int ID = -1;
  
  public Throwable error;
  public Message cause;
  
  public ErrorMessage (Throwable error, Message cause)
  {
    this.error = error;
    this.cause = cause;
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
    throw new UnsupportedOperationException ();
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    throw new UnsupportedOperationException ();
  }
}
