package org.avis.net;

import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolDecoderOutput;
import org.apache.mina.protocol.ProtocolEncoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;
import org.apache.mina.protocol.codec.MessageDecoder;
import org.apache.mina.protocol.codec.MessageDecoderResult;
import org.apache.mina.protocol.codec.MessageEncoder;

import org.avis.net.messages.ConfConn;
import org.avis.net.messages.ConnRply;
import org.avis.net.messages.ConnRqst;
import org.avis.net.messages.Disconn;
import org.avis.net.messages.DisconnRply;
import org.avis.net.messages.DisconnRqst;
import org.avis.net.messages.Message;
import org.avis.net.messages.Nack;
import org.avis.net.messages.NotifyDeliver;
import org.avis.net.messages.NotifyEmit;
import org.avis.net.messages.QuenchPlaceHolder;
import org.avis.net.messages.SecRply;
import org.avis.net.messages.SecRqst;
import org.avis.net.messages.SubAddRqst;
import org.avis.net.messages.SubDelRqst;
import org.avis.net.messages.SubModRqst;
import org.avis.net.messages.SubRply;
import org.avis.net.messages.TestConn;
import org.avis.net.messages.UNotify;

import static dsto.dfc.logging.Log.warn;

/**
 * Codec for Elvin message frames. Reads/writes messages to/from the
 * Elvin XDR frame format with the help of
 * {@link Message#decode(ByteBuffer)} and
 * {@link Message#encode(ByteBuffer)}.
 * 
 * @author Matthew Phillips
 */
public class FrameCodec implements MessageDecoder, MessageEncoder
{
  private static final Set<Class> MESSAGE_TYPES;
  // private static final int MAX_BUFFER_DUMP = 512;
  
  static
  {
    MESSAGE_TYPES = new HashSet<Class> ();
    
    MESSAGE_TYPES.add (ConnRqst.class);
    MESSAGE_TYPES.add (ConnRply.class);
    MESSAGE_TYPES.add (DisconnRqst.class);
    MESSAGE_TYPES.add (DisconnRply.class);
    MESSAGE_TYPES.add (Disconn.class);
    MESSAGE_TYPES.add (SubAddRqst.class);
    MESSAGE_TYPES.add (SubModRqst.class);
    MESSAGE_TYPES.add (SubRply.class);
    MESSAGE_TYPES.add (SubDelRqst.class);
    MESSAGE_TYPES.add (Nack.class);
    MESSAGE_TYPES.add (NotifyDeliver.class);
    MESSAGE_TYPES.add (NotifyEmit.class);
    MESSAGE_TYPES.add (TestConn.class);
    MESSAGE_TYPES.add (ConfConn.class);
    MESSAGE_TYPES.add (SecRqst.class);
    MESSAGE_TYPES.add (SecRply.class);
    MESSAGE_TYPES.add (UNotify.class);
  }

  public void encode (ProtocolSession session,
                      Object messageObject,
                      ProtocolEncoderOutput out)
    throws ProtocolViolationException
  {
    ByteBuffer buffer = ByteBuffer.allocate (4096); // auto deallocated
    buffer.setAutoExpand (true);
    
    // leave room for frame size
    buffer.position (4);
    
    Message message = (Message)messageObject;

    // write frame type
    buffer.putInt (message.typeId ());
    
    message.encode (buffer);

    int frameSize = buffer.position () - 4;
    
    // write frame size
    buffer.putInt (0, frameSize);
    
    // write out whole frame
    buffer.flip ();
    
    // if (isEnabled (TRACE) && buffer.limit () <= MAX_BUFFER_DUMP)
    //  trace ("Codec output: " + buffer.getHexDump (), this);
    
    // sanity check frame is 4-byte aligned
    if (frameSize % 4 != 0)
      throw new ProtocolViolationException
        ("Frame length not 4 byte aligned for " + message.getClass ());
    
    out.write (buffer);
  }

  public MessageDecoderResult decodable (ProtocolSession session,
                                         ByteBuffer in)
  {
    if (in.remaining () < 4)
      return NEED_DATA;
    
    int frameSize = in.getInt ();
    
    if (in.remaining () < frameSize)
    {
      if (in.limit () < frameSize + 4)
        in.limit (frameSize + 4);
      
      return NEED_DATA;
    } else
    {
      return OK;
    }
  }

  public MessageDecoderResult decode (ProtocolSession session,
                                      ByteBuffer in,
                                      ProtocolDecoderOutput out)
    throws ProtocolViolationException
  {
    // if (isEnabled (TRACE) && in.limit () <= MAX_BUFFER_DUMP)
    //  trace ("Codec input: " + in.getHexDump (), this);
    
    int frameSize = in.getInt ();
    int dataStart = in.position ();
    int messageType = in.getInt ();

    // if (isEnabled (TRACE))
    //  trace ("Frame length: " + frameSize, this);

    Message message;
    
    switch (messageType)
    {
      case ConnRqst.ID:
        message = new ConnRqst ();
        break;
      case ConnRply.ID:
        message = new ConnRply ();
        break;
      case DisconnRqst.ID:
        message = new DisconnRqst ();
        break;
      case DisconnRply.ID:
        message = new DisconnRply ();
        break;
      case Disconn.ID:
        message = new Disconn ();
        break;
      case SubAddRqst.ID:
        message = new SubAddRqst ();
        break;
      case SubRply.ID:
        message = new SubRply ();
        break;
      case SubModRqst.ID:
        message = new SubModRqst ();
        break;
      case SubDelRqst.ID:
        message = new SubDelRqst ();
        break;
      case Nack.ID:
        message = new Nack ();
        break;
      case NotifyDeliver.ID:
        message = new NotifyDeliver ();
        break;
      case NotifyEmit.ID:
        message = new NotifyEmit ();
        break;
      case TestConn.ID:
        message = new TestConn ();
        break;
      case ConfConn.ID:
        message = new ConfConn ();
        break;
      case SecRqst.ID:
        message = new SecRqst ();
        break;
      case SecRply.ID:
        message = new SecRply ();
        break;
      case UNotify.ID:
        message = new UNotify ();
        break;
      case QuenchPlaceHolder.ADD:
      case QuenchPlaceHolder.MODIFY:
      case QuenchPlaceHolder.DELETE:
        message = new QuenchPlaceHolder (messageType, frameSize - 4);
        break;
      default:
        throw new ProtocolViolationException
          ("Unknown message type: ID = " + messageType);
    }
    
    if (frameSize % 4 != 0)
      throw new ProtocolViolationException
        ("Frame length not 4 byte aligned for " + message);
    
    // todo NACK on protocol violation and underflow using error message
    message.decode (in);
    
    int remainder = frameSize - (in.position () - dataStart);
    
    if (remainder > 0)
    {
      warn ("Some input not read by " + message.getClass () +
            " (" + remainder + " bytes)", this);
      
      in.skip (remainder);
    }
    
    out.write (message);
    
    return OK;
  }

  public Set<Class> getMessageTypes ()
  {
    return MESSAGE_TYPES;
  }
}
