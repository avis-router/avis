package org.avis.io;

import java.nio.BufferUnderflowException;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import org.avis.io.messages.ConfConn;
import org.avis.io.messages.ConnRply;
import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.DisconnRply;
import org.avis.io.messages.DisconnRqst;
import org.avis.io.messages.ErrorMessage;
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
import org.avis.io.messages.XidMessage;

import static org.avis.logging.Log.internalError;

/**
 * Codec for Elvin message frames. Reads/writes messages to/from the
 * Elvin XDR frame format with the help of
 * {@link Message#decode(ByteBuffer)} and
 * {@link Message#encode(ByteBuffer)}.
 * 
 * @author Matthew Phillips
 */
public class FrameCodec
  extends CumulativeProtocolDecoder
  implements ProtocolEncoder, ProtocolDecoder, ProtocolCodecFactory
{
  public static final FrameCodec INSTANCE = new FrameCodec ();

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
  
  public void encode (IoSession session,
                      Object messageObject,
                      ProtocolEncoderOutput out)
    throws Exception
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
    
    // if (isEnabled (TRACE) && buffer.limit () <= MAX_BUFFER_DUMP)
    //  trace ("Codec output: " + buffer.getHexDump (), this);
    
    // sanity check frame is 4-byte aligned
    if (frameSize % 4 != 0)
      throw new ProtocolCodecException
        ("Frame length not 4 byte aligned for " + message.getClass ());
    
    int maxLength = maxFrameLengthFor (session);
    
    if (frameSize <= maxLength)
    {
      // write out whole frame
      buffer.flip ();
      out.write (buffer);
    } else
    {
      throw new ProtocolCodecException
        ("Frame size of " + frameSize + " bytes is larger than maximum " + 
         maxLength);
    }
  }

  protected static boolean haveFullFrame (IoSession session,
                                          ByteBuffer in)
  {
    if (in.remaining () < 4)
      return false;
    
    boolean haveFrame;
    int start = in.position ();
    
    int frameSize = in.getInt ();
    
    if (frameSize > maxFrameLengthFor (session))
    {
      // when frame too big, OK it and let decode () generate error
      haveFrame = true;
    } else if (in.remaining () < frameSize)
    {
      if (in.capacity () < frameSize + 4)
      {
        // need to save and restore limit
        int limit = in.limit ();
        
        in.expand (frameSize);
      
        in.limit (limit);
      }
      
      haveFrame = false;
    } else
    {
      haveFrame = true;
    }

    in.position (start);
    
    return haveFrame;
  }

  protected boolean doDecode (IoSession session,
                              ByteBuffer in,
                              ProtocolDecoderOutput out)
    throws Exception
  {
    // if (isEnabled (TRACE) && in.limit () <= MAX_BUFFER_DUMP)
    //  trace ("Codec input: " + in.getHexDump (), this);
    
    if (!haveFullFrame (session, in))
      return false;
    
    int maxLength = maxFrameLengthFor (session);
    
    int frameSize = in.getInt ();
    int dataStart = in.position ();

    Message message = null;
    
    try
    {
      int messageType = in.getInt ();
      
      message = newMessage (messageType, frameSize);
    
      if (frameSize % 4 != 0)
        throw new ProtocolCodecException
          ("Frame length not 4 byte aligned");
      
      if (frameSize > maxLength)
        throw new ProtocolCodecException
          ("Frame size of " + frameSize + " bytes is larger than maximum " + 
           maxLength);
      
      message.decode (in);
      
      int remainder = frameSize - (in.position () - dataStart);
      
      if (remainder > 0)
      {
        internalError ("Some input not read by " + message.getClass () +
                       " (" + remainder + " bytes)", this);
        
        in.skip (remainder);
      }
    } catch (Exception ex)
    {
      // handle client protocol violations by generating an ErrorMessage
      if (ex instanceof ProtocolCodecException ||
          ex instanceof BufferUnderflowException)
      {
        ErrorMessage error = new ErrorMessage (ex, message);
        
        // fill in XID if possible
        if (message instanceof XidMessage && in.remaining () >= 4)
          ((XidMessage)message).xid = in.getInt ();
        
        in.skip (in.remaining ());
        
        message = error;
      } else
      {
        throw (RuntimeException)ex;
      }
    }
    
    out.write (message);
    
    return true;
  }
  

  public void finishDecode (IoSession session, ProtocolDecoderOutput out)
    throws Exception
  {
    // zip
  }
  
  /**
   * Create a new message for a given type code.
   */
  private static Message newMessage (int messageType, int frameSize)
    throws ProtocolCodecException
  {
    Message message = null;
    
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
        throw new ProtocolCodecException
          ("Unknown message type: ID = " + messageType);
    }
    
    return message;
  }
  
  private static int maxFrameLengthFor (IoSession session)
  {
    Integer length = (Integer)session.getAttribute ("maxFrameLength");
    
    if (length == null)
      return Integer.MAX_VALUE;
    else
      return length;
  }
  
  public static void setMaxFrameLengthFor (IoSession session, int length)
  {
    session.setAttribute ("maxFrameLength", length);
  }
}
