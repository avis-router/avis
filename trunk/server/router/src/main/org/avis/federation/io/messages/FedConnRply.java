package org.avis.federation.io.messages;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.messages.XidMessage;

import static org.avis.io.XdrCoding.getString;
import static org.avis.io.XdrCoding.putString;

public class FedConnRply extends XidMessage
{
  public static final int ID = 193;
  
  public String serverDomain;
  
  public FedConnRply ()
  {
    // zip
  }
  
  public FedConnRply (FedConnRqst request, String serverDomain)
  {
    super (request);
    
    this.serverDomain = serverDomain;
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public void encode (IoBuffer out)
    throws ProtocolCodecException
  {
    super.encode (out);
    
    putString (out, serverDomain);
  }
  
  @Override
  public void decode (IoBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    serverDomain = getString (in);
  }
}
