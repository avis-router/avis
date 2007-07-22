package org.avis.federation.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.messages.RequestMessage;
import org.avis.subscription.ast.Node;

public class FedModify extends RequestMessage<Ack>
{
  public static final int ID = 194;
  
  public Node incomingFilter;
  
  public FedModify ()
  {
    // zip
  }
  
  public FedModify (Node incomingFilter)
  {
    this.incomingFilter = incomingFilter;
  }

  @Override
  public Class<Ack> replyType ()
  {
    return Ack.class;
  }

  @Override
  public int typeId ()
  {
    return 194;
  }
  
  @Override
  public void encode (ByteBuffer out) throws ProtocolCodecException
  {
    super.encode (out);
    
    // todo
    // FederationIO.encodeAST (out, incomingFilter);
  }
  
  @Override
  public void decode (ByteBuffer in) throws ProtocolCodecException
  {
    super.decode (in);
    
    // todo
    // incomingFilter = FederationIO.decodeAST (in);
  }
}
