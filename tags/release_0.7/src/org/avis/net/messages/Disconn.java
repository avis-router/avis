package org.avis.net.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import static org.avis.net.IO.getString;
import static org.avis.net.IO.putString;

public class Disconn extends Message
{
  public static final int ID = 53;

  public static final int REASON_SHUTDOWN = 1;
  public static final int REASON_SHUTDOWN_REDIRECT = 2;
  public static final int REASON_PROTOCOL_VIOLATION = 4;
  
  public int reason;
  public String args;
  
  public Disconn ()
  {
    this (-1, "");
  }
  
  public Disconn (int reason, String args)
  {
    this.reason = reason;
    this.args = args;
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
    reason = in.getInt ();
    args = getString (in);
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    if (reason == -1)
      throw new ProtocolCodecException ("Reason not set");
    
    out.putInt (reason);
    putString (out, args);
  }
}
