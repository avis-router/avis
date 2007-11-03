package org.avis.federation.io.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.messages.RequestMessage;
import org.avis.subscription.ast.Node;

import static org.avis.federation.io.XdrAstCoding.decodeAST;
import static org.avis.federation.io.XdrAstCoding.encodeAST;

public class FedSubReplace extends RequestMessage<Ack>
{
  public static final int ID = 194;
  
  public Node incomingFilter;
  
  public FedSubReplace ()
  {
    // zip
  }
  
  public FedSubReplace (Node incomingFilter)
  {
    super (nextXid ());
    
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
    return ID;
  }
  
  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    super.encode (out);
    
    encodeAST (out, incomingFilter);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    incomingFilter = decodeAST (in);
  }
}
