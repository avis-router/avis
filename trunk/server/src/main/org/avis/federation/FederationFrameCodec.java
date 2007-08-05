package org.avis.federation;

import org.apache.mina.common.IoFilter;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import org.avis.federation.messages.Ack;
import org.avis.federation.messages.FedConnRply;
import org.avis.federation.messages.FedConnRqst;
import org.avis.federation.messages.FedModify;
import org.avis.federation.messages.FedNotify;
import org.avis.io.FrameCodec;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;

public class FederationFrameCodec
  extends FrameCodec implements ProtocolCodecFactory
{
  private static final FederationFrameCodec INSTANCE =
    new FederationFrameCodec ();
  
  public static final IoFilter FILTER = new ProtocolCodecFilter (INSTANCE);

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
      case Nack.ID:
        return new Nack ();
      case Disconn.ID:
        return new Disconn ();
      case Ack.ID:
        return new Ack ();
      case FedConnRply.ID:
        return new FedConnRply ();
      case FedConnRqst.ID:
        return new FedConnRqst ();
      case FedModify.ID:
        return new FedModify ();
      case FedNotify.ID:
        return new FedNotify ();
      default:
        throw new ProtocolCodecException
          ("Unknown message type: ID = " + messageType);
    }
  }
}
