package org.avis.federation.io.messages;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.messages.RequestMessage;

import static org.avis.io.XdrCoding.getString;
import static org.avis.io.XdrCoding.putString;

public class FedConnRqst extends RequestMessage<FedConnRply>
{
  public static final int ID = 192;
  
  public int versionMajor;
  public int versionMinor;
  public String serverDomain;
  
  public FedConnRqst ()
  {
    // zip
  }

  public FedConnRqst (int versionMajor, int versionMinor, String serverDomain)
  {
    super (nextXid ());
    
    this.versionMajor = versionMajor;
    this.versionMinor = versionMinor;
    this.serverDomain = serverDomain;
  }

  @Override
  public Class<FedConnRply> replyType ()
  {
    return FedConnRply.class;
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
    
    out.putInt (versionMajor);
    out.putInt (versionMinor);
    putString (out, serverDomain);
  }
  
  @Override
  public void decode (IoBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    versionMajor = in.getInt ();
    versionMinor = in.getInt ();
    serverDomain = getString (in);
  }
}
