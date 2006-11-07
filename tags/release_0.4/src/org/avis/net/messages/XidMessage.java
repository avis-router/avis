package org.avis.net.messages;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolViolationException;

/**
 * Base class for messages that use a transaction id to identify replies.
 * 
 * @author Matthew Phillips
 */
public abstract class XidMessage extends Message
{
  private static final AtomicInteger xidCounter = new AtomicInteger ();
  
  public int xid;
  
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
    throws ProtocolViolationException
  {
    xid = in.getInt ();
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolViolationException
  {
    if (xid == -1)
      throw new ProtocolViolationException ("No XID");
    
    out.putInt (xid);
  }
  
  protected static int nextXid ()
  {
    // NOTE: XID must not be zero (sec 7.4)
    return xidCounter.incrementAndGet ();
  }
}
