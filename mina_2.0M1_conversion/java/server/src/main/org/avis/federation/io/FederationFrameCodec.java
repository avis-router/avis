package org.avis.federation.io;

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import org.avis.federation.io.messages.Ack;
import org.avis.federation.io.messages.FedConnRply;
import org.avis.federation.io.messages.FedConnRqst;
import org.avis.federation.io.messages.FedNotify;
import org.avis.federation.io.messages.FedSubReplace;
import org.avis.io.FrameCodec;
import org.avis.io.messages.ConfConn;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.DropWarn;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.TestConn;

public class FederationFrameCodec
  extends FrameCodec implements ProtocolCodecFactory
{
  private static final FederationFrameCodec INSTANCE =
    new FederationFrameCodec ();
  
  public static final IoFilter FILTER = new ProtocolCodecFilter (INSTANCE);

  public ProtocolEncoder getEncoder (IoSession session)
    throws Exception
  {
    return INSTANCE;
  }
  
  public ProtocolDecoder getDecoder (IoSession session)
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
      case FedSubReplace.ID:
        return new FedSubReplace ();
      case FedNotify.ID:
        return new FedNotify ();
      case TestConn.ID:
        return TestConn.INSTANCE;
      case ConfConn.ID:
        return ConfConn.INSTANCE;
      case DropWarn.ID:
        return new DropWarn ();
      default:
        throw new ProtocolCodecException
          ("Unknown message type: ID = " + messageType);
    }
  }
}
