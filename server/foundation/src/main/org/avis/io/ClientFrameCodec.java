package org.avis.io;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import org.avis.io.messages.ConfConn;
import org.avis.io.messages.ConnRply;
import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.DisconnRply;
import org.avis.io.messages.DisconnRqst;
import org.avis.io.messages.DropWarn;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.NotifyDeliver;
import org.avis.io.messages.NotifyEmit;
import org.avis.io.messages.QuenchPlaceHolder;
import org.avis.io.messages.SecRply;
import org.avis.io.messages.SecRqst;
import org.avis.io.messages.SubAddRqst;
import org.avis.io.messages.SubDelRqst;
import org.avis.io.messages.SubModRqst;
import org.avis.io.messages.SubRply;
import org.avis.io.messages.TestConn;
import org.avis.io.messages.UNotify;

/**
 * Codec for Elvin client protocol message frames.
 * 
 * @author Matthew Phillips
 */
public class ClientFrameCodec
  extends FrameCodec implements ProtocolCodecFactory
{
  public static final ClientFrameCodec INSTANCE = new ClientFrameCodec ();

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
      case ConnRqst.ID:
        return new ConnRqst ();
      case ConnRply.ID:
        return new ConnRply ();
      case DisconnRqst.ID:
        return new DisconnRqst ();
      case DisconnRply.ID:
        return new DisconnRply ();
      case Disconn.ID:
        return new Disconn ();
      case SubAddRqst.ID:
        return new SubAddRqst ();
      case SubRply.ID:
        return new SubRply ();
      case SubModRqst.ID:
        return new SubModRqst ();
      case SubDelRqst.ID:
        return new SubDelRqst ();
      case Nack.ID:
        return new Nack ();
      case NotifyDeliver.ID:
        return new NotifyDeliver ();
      case NotifyEmit.ID:
        return new NotifyEmit ();
      case TestConn.ID:
        return TestConn.INSTANCE;
      case ConfConn.ID:
        return ConfConn.INSTANCE;
      case SecRqst.ID:
        return new SecRqst ();
      case SecRply.ID:
        return new SecRply ();
      case UNotify.ID:
        return new UNotify ();
      case DropWarn.ID:
        return new DropWarn ();
      case QuenchPlaceHolder.ADD:
      case QuenchPlaceHolder.MODIFY:
      case QuenchPlaceHolder.DELETE:
        return new QuenchPlaceHolder (messageType, frameSize - 4);
      default:
        throw new ProtocolCodecException
          ("Unknown message type: ID = " + messageType);
    }
  }
}
