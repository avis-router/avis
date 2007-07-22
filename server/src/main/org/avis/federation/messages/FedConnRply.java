package org.avis.federation.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.messages.XidMessage;

import static org.avis.io.IO.getString;
import static org.avis.io.IO.putString;

public class FedConnRply extends XidMessage
{
  public static final int ID = 193;
  
  public String serverDomain;
  
  public FedConnRply ()
  {
    // zip
  }
  
  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    super.encode (out);
    
    putString (out, serverDomain);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    serverDomain = getString (in);
  }
}
