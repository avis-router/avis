package org.avis.io.messages;

/**
 * A XID-based request message that has a defined reply message.
 * 
 * @author Matthew Phillips
 */
public abstract class RequestMessage<R extends XidMessage> extends XidMessage
{
  public RequestMessage ()
  {
    super ();
  }

  public RequestMessage (int xid)
  {
    super (xid);
  }

  /**
   * The type of a successful reply.
   */
  public abstract Class<R> replyType ();
}
