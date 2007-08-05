package org.avis.io.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

/**
 * Synthetic message generated when a request timeout has elapsed.
 * 
 * @author Matthew Phillips
 */
public class RequestTimeoutMessage extends Message
{
  public static final int ID = -2;
  
  /**
   * The request that timed out.
   */
  public final RequestMessage<?> request;

  public RequestTimeoutMessage (RequestMessage<?> request)
  {
    this.request = request;
  }

  @Override
  public void decode (ByteBuffer in) 
    throws ProtocolCodecException
  {
    throw new UnsupportedOperationException ("Synthetic message");
  }

  @Override
  public void encode (ByteBuffer out) 
    throws ProtocolCodecException
  {
    throw new UnsupportedOperationException ("Synthetic message");
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
}
