package org.avis.net;

import java.util.HashSet;
import java.util.Set;

import java.nio.BufferUnderflowException;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.apache.mina.filter.codec.demux.MessageEncoder;

import org.avis.net.messages.ConfConn;
import org.avis.net.messages.ConnRply;
import org.avis.net.messages.ConnRqst;
import org.avis.net.messages.Disconn;
import org.avis.net.messages.DisconnRply;
import org.avis.net.messages.DisconnRqst;
import org.avis.net.messages.ErrorMessage;
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
import org.avis.net.messages.XidMessage;

import static dsto.dfc.logging.Log.internalError;

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
  private static final ConnectionOptions DEFAULT_OPTIONS =
    new ConnectionOptions ();
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
  
  public Set<Class> getMessageTypes ()
  {
    return MESSAGE_TYPES;
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
    
    ConnectionOptions options = optionsFor (session);
    
    if (frameSize <= options.getInt ("Packet.Max-Length"))
    {
      // write out whole frame
      buffer.flip ();
      out.write (buffer);
    } else
    {
      throw new ProtocolCodecException
        ("Frame size of " + frameSize + " bytes is larger than maximum " + 
            options.getInt ("Packet.Max-Length"));
    }
  }

  public MessageDecoderResult decodable (IoSession session,
                                         ByteBuffer in)
  {
    if (in.remaining () < 4)
      return NEED_DATA;
    
    int frameSize = in.getInt ();
    
    ConnectionOptions options = optionsFor (session);
    
    if (frameSize > options.getInt ("Packet.Max-Length"))
    {
      // when frame too big, OK it and let decode () generate error
      in.limit (options.getInt ("Packet.Max-Length"));
      
      return OK;
    } else if (in.remaining () < frameSize)
    {
      if (in.limit () < frameSize + 4)
        in.limit (frameSize + 4);
      
      return NEED_DATA;
    } else
    {
      return OK;
    }
  }

  public MessageDecoderResult decode (IoSession session,
                                      ByteBuffer in,
                                      ProtocolDecoderOutput out)
    throws Exception
  {
    // if (isEnabled (TRACE) && in.limit () <= MAX_BUFFER_DUMP)
    //  trace ("Codec input: " + in.getHexDump (), this);
    
    ConnectionOptions options = optionsFor (session);
    
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
      
      if (frameSize > options.getInt ("Packet.Max-Length"))
        throw new ProtocolCodecException
          ("Frame size of " + frameSize + " bytes is larger than maximum " + 
           options.getInt ("Packet.Max-Length"));
      
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
    
    return OK;
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
  
  /**
   * Get the connection options set for the given session. Returns
   * defaults if none set.
   */
  private static ConnectionOptions optionsFor (IoSession session)
  {
    ConnectionOptions options =
      (ConnectionOptions)session.getAttribute ("connectionOptions");
    
    if (options == null)
      options = DEFAULT_OPTIONS;
    
    return options;
  }

  /**
   * Set the connection options for a given session.
   */
  public static void setOptions (IoSession session,
                                 ConnectionOptions options)
  {
    session.setAttribute ("connectionOptions", options);
  }
}
