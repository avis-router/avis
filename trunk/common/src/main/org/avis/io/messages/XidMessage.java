package org.avis.io.messages;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.RequestTrackingFilter;

/**
 * Base class for messages that use a transaction id to identify replies.
 * 
 * @author Matthew Phillips
 */
public abstract class XidMessage extends Message
{
  private static final AtomicInteger xidCounter = new AtomicInteger ();
  
  public int xid;
  
  /**
   * The request message that triggered this reply. This is for the
   * convenience of message processing, not part of the serialized
   * format: you need to add a {@link RequestTrackingFilter} to the
   * filter chain if you want this automatically filled in.
   */
  public transient RequestMessage<?> request;
  
  public XidMessage ()
  {
    xid = -1;
  }
  
  public XidMessage (XidMessage inReplyTo)
  {
    this (inReplyTo.xid);
  }

  public XidMessage (int xid)
  {
    this.xid = xid;
  }

  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    xid = in.getInt ();
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    if (xid == -1)
      throw new ProtocolCodecException ("No XID");
    
    out.putInt (xid);
  }
  
  protected static int nextXid ()
  {
    // NOTE: XID must not be zero (sec 7.4)
    return xidCounter.incrementAndGet ();
  }
}
