package org.avis.io;

import java.nio.BufferUnderflowException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.NotifyDeliver;
import org.avis.io.messages.XidMessage;

import static org.avis.io.XdrCoding.putLongArray;

/**
 * Base class for Elvin XDR frame codecs. Reads/writes messages
 * to/from the Elvin XDR frame format with the help of
 * {@link Message#decode(IoBuffer)} and
 * {@link Message#encode(IoBuffer)}. Understood message sets are
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
    // check if we can optimise a NotifyDeliver
    if (messageObject instanceof NotifyDeliver)
    {
      NotifyDeliver notifyDeliver = (NotifyDeliver)messageObject;
      
      if (notifyDeliver.original != null && 
          notifyDeliver.original.rawAttributes != null)
      {
        encodeOptimisedNotifyDeliver (out, notifyDeliver);
        
        return;
      }
    }
    
    // buffer is auto deallocated
    IoBuffer buffer = IoBuffer.allocate (4096); 
    buffer.setAutoExpand (true);
    
    // leave room for frame size
    buffer.position (4);
    
    Message message = (Message)messageObject;
  
    // write frame type
    buffer.putInt (message.typeId ());
    
    message.encode (buffer);
  
    int frameSize = buffer.position () - 4;
    
    message.frameSize = frameSize;
    
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
      throw new FrameTooLargeException (maxLength, frameSize);
    }
  }

  /**
   * Optimised encoding for NotifyDeliver which avoids re-serialising
   * attributes: we just output the raw serialised attributes we read
   * in for the Notify. This avoids wasting a lot of memory exploding
   * duplicate attributes out when a notification is sent to many
   * clients, and saves time not re-serialising attributes.
   */
  private static void encodeOptimisedNotifyDeliver (ProtocolEncoderOutput out,
                                                    NotifyDeliver notifyDeliver)
  {
    IoBuffer headerBuffer = IoBuffer.allocate (20);
    IoBuffer matchesBuffer = IoBuffer.allocate (256);
    
    matchesBuffer.setAutoExpand (true);
    
    putLongArray (matchesBuffer, notifyDeliver.secureMatches);
    putLongArray (matchesBuffer, notifyDeliver.insecureMatches);
    
    matchesBuffer.flip ();
    
    int frameSize = 
      4 + notifyDeliver.original.rawAttributes.limit () + matchesBuffer.limit ();
    
    notifyDeliver.frameSize = frameSize;
    
    headerBuffer.putInt (frameSize);
    headerBuffer.putInt (NotifyDeliver.ID);
    
    headerBuffer.flip ();

    out.write (headerBuffer);
    out.write (notifyDeliver.original.rawAttributes.asReadOnlyBuffer ());
    out.write (matchesBuffer);

//    // consolidate and write
//    IoBuffer buff = IoBuffer.allocate (frameSize + 4);
//    
//    buff.put (headerBuffer);
//    buff.put (notifyDeliver.original.rawAttributes.asReadOnlyBuffer ());
//    buff.put (matchesBuffer);
//    
//    headerBuffer.free ();
//    matchesBuffer.free ();
//    
//    buff.flip ();
//    out.write (buff);
        
//    IoBuffer rawAttrs = IoBuffer.allocate (notifyDeliver.original.rawAttributes.limit ());

    
//    // reuse array
//    byte [] bytes = new byte [notifyDeliver.original.rawAttributes.limit ()];
//    System.arraycopy (notifyDeliver.original.rawAttributes.array (), 0, bytes, 0, bytes.length);
//    
//    rawAttrs.put (bytes);

    
//    // reserialise
//    try
//    {
//      XdrCoding.putNameValues (rawAttrs, notifyDeliver.attributes);
//    } catch (ProtocolCodecException ex)
//    {
//      ex.printStackTrace ();
//    }
    
    
//    rawAttrs.flip ();
//    
//    out.write (headerBuffer);
//    out.write (rawAttrs);
//    out.write (matchesBuffer);    
  }

  @Override
  public void decode (IoSession session, IoBuffer in,
                      ProtocolDecoderOutput out)
    throws Exception
  {
    try
    {
      super.decode (session, in, out);
    } catch (Throwable t)
    {
      /*
       * Stop ProtocolCodecFilter in MINA 2.0M6 blowing heap with hex
       * dumps (see ProtocolCodecFilter.messageReceived () exception
       * handler)
       */
      ProtocolDecoderException pde;
      
      if (t instanceof ProtocolDecoderException)
        pde = (ProtocolDecoderException)t;
      else
        pde = new ProtocolDecoderException (t);
      
      pde.setHexdump ("<none>");
      
      throw pde;
    }
  }
  
  @Override
  protected boolean doDecode (IoSession session, IoBuffer in,
                              ProtocolDecoderOutput out)
    throws Exception
  {
    // if (isEnabled (TRACE) && in.limit () <= MAX_BUFFER_DUMP)
    //  trace ("Codec input: " + in.getHexDump (), this);
    
    // if in protocol violation mode, do not try to read any further
    if (session.getAttribute ("protocolViolation") != null)
      return false;
    
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
      
      message.frameSize = frameSize;
      
      message.decode (in);
      
      int bytesRead = in.position () - dataStart;
      
      if (bytesRead != frameSize)
      {
        throw new ProtocolCodecException 
          ("Some input not read for " + message.name () + ": " +
           "Frame header said " + frameSize + 
           " bytes, but only read " + bytesRead);
      }
      
      out.write (message);
    
      return true;
    } catch (Exception ex)
    {
      if (ex instanceof ProtocolCodecException ||
          ex instanceof BufferUnderflowException ||
          ex instanceof FrameTooLargeException)
      {
        /*
         * Mark session in violation and handle once: codec will only
         * generate one error message, it's up to consumer to try to
         * recover or close connection.
         */
        session.setAttribute ("protocolViolation");
        session.suspendRead ();
        
        ErrorMessage error = new ErrorMessage (ex, message); 
        
        // fill in XID if possible
        if (message instanceof XidMessage && in.limit () >= 12)
        {
          int xid = in.getInt (8);
          
          if (xid > 0)
            ((XidMessage)message).xid = xid;
        }

        out.write (error);
        
        return true;
      } else
      {
        throw (RuntimeException)ex;
      }
    }
  }

  /**
   * Create a new instance of a message given a message type code and
   * frame length.
   */
  protected abstract Message newMessage (int messageType, int frameSize)
    throws ProtocolCodecException;
  
  private static boolean haveFullFrame (IoSession session, IoBuffer in)
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
        
        in.expand (frameSize + 4);
      
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

  public static void setMaxFrameLengthFor (IoSession session, int length)
  {
    session.setAttribute ("maxFrameLength", length);
  }

  private static int maxFrameLengthFor (IoSession session)
  {
    Integer length = (Integer)session.getAttribute ("maxFrameLength");
    
    if (length == null)
      return Integer.MAX_VALUE;
    else
      return length;
  }
}