package org.avis.federation.io.messages;

import org.avis.io.messages.XidMessage;

public class Ack extends XidMessage
{
  public static final int ID = 65;

  public Ack ()
  {
    // zip
  }

  public Ack (XidMessage inReplyTo)
  {
    super (inReplyTo);
  }

  public Ack (int xid)
  {
    super (xid);
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
}
