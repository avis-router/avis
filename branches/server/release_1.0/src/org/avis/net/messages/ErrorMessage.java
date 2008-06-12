package org.avis.net.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.net.common.FrameCodec;

/**
 * Placeholder message generated by {@link FrameCodec} to signal
 * protocol errors.
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

  /**
   * Generate an error message suitable for presentation as a
   * debugging aid.
   */
  public String formattedMessage ()
  {
    StringBuilder message = new StringBuilder ();
    
    if (cause == null)
      message.append ("Error decoding XDR frame");
    else
      message.append ("Error decoding ").append (cause);
    
    if (error != null)
    {
      message.append (": ").append (error.getClass ().getName ());
      
      if (error.getMessage () != null)
        message.append (": ").append (error.getMessage ());
    }
    
    return message.toString (); 
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