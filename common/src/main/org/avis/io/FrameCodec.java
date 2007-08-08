package org.avis.io;

import java.nio.BufferUnderflowException;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.XidMessage;

import static org.avis.logging.Log.internalError;

/**
 * Base class for Elvin XDR frame codecs. Reads/writes messages
 * to/from the Elvin XDR frame format with the help of
 * {@link Message#decode(ByteBuffer)} and
 * {@link Message#encode(ByteBuffer)}. Understood message sets are
 * effectively defined by the subclasses' implementation of
 * {@link #newMessage(int, int)}.
 * 
 * @author Matthew Phillips
 */
public abstract class FrameCodec
  extends CumulativeProtocolDecoder implements ProtocolEncoder
{
  public void encode (IoSession session, Object messageObject,
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

  @Override
  protected boolean doDecode (IoSession session, ByteBuffer in,
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
        throw new ProtocolCodecException ("Frame length not 4 byte aligned");
      
      if (frameSize > maxLength)
        throw new FrameTooLargeException (maxLength, frameSize);
      
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
          ex instanceof BufferUnderflowException ||
          ex instanceof FrameTooLargeException)
      {
        ErrorMessage error = new ErrorMessage (ex, message);
        
        // fill in XID if possible
        if (message instanceof XidMessage && in.remaining () >= 4)
          ((XidMessage)message).xid = in.getInt ();
        
        in.skip (in.remaining ());
        
        session.suspendRead ();

        message = error;
      } else
      {
        throw (RuntimeException)ex;
      }
    }
    
    out.write (message);
    
    return true;
  }

  /**
   * Create a new instance of a message given a message type code and
   * frame length.
   */
  protected abstract Message newMessage (int messageType, int frameSize)
    throws ProtocolCodecException;
  
  private static boolean haveFullFrame (IoSession session, ByteBuffer in)
  {
    // need frame size and type before we do anything
    if (in.remaining () < 8)
      return false;
    
    boolean haveFrame;
    int start = in.position ();
    
    int frameSize = in.getInt ();
    
    if (frameSize > maxFrameLengthFor (session))
    {
      // when frame too big, OK it and let doDecode () generate error
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

  @Override
  public void finishDecode (IoSession session, ProtocolDecoderOutput out)
    throws Exception
  {
    // zip
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