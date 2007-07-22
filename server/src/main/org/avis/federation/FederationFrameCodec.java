package org.avis.federation;

import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import org.avis.federation.messages.Ack;
import org.avis.federation.messages.FedConnRply;
import org.avis.federation.messages.FedConnRqst;
import org.avis.federation.messages.FedModify;
import org.avis.io.FrameCodec;
import org.avis.io.messages.Message;

public class FederationFrameCodec
  extends FrameCodec implements ProtocolCodecFactory
{
  public static final FederationFrameCodec INSTANCE =
    new FederationFrameCodec ();

  public ProtocolEncoder getEncoder ()
    throws Exception
  {
    return INSTANCE;
  }
  
  public ProtocolDecoder getDecoder ()
    throws Exception
  {
    return INSTANCE;
  }
  
  @Override
  protected Message newMessage (int messageType, int frameSize)
    throws ProtocolCodecException
  {
    switch (messageType)
    {
      case Ack.ID:
        return new Ack ();
      case FedConnRply.ID:
        return new FedConnRply ();
      case FedConnRqst.ID:
        return new FedConnRqst ();
      case FedModify.ID:
        return new FedModify ();
      default:
        throw new ProtocolCodecException
          ("Unknown message type: ID = " + messageType);
    }
  }
}
