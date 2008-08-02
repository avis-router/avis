package org.avis.io.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

/**
 * Placeholder for QnchAddRqst, QnchModRqst and QnchDelRqst that
 * allows them to be decoded and sent to server. Server will currently
 * NACK.
 * 
 * @author Matthew Phillips
 */
public class QuenchPlaceHolder extends XidMessage
{
  public static final int ID = -2;
  
  public static final int ADD = 80;
  public static final int MODIFY = 81;
  public static final int DELETE = 82;
  
  public int messageType;
  public int length;

  public QuenchPlaceHolder (int messageType, int length)
  {
    this.messageType = messageType;
    this.length = length;
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
    super.decode (in);
    
    in.skip (length - 4);
  }
  
  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    throw new UnsupportedOperationException
      ("This is just a quench placeholder for now");
  }
}
