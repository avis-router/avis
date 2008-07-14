package org.avis.io.messages;

public class DisconnRply extends XidMessage
{
  public static final int ID = 52;
  
  public DisconnRply ()
  {
    // zip
  }
  
  public DisconnRply (DisconnRqst inReplyTo)
  {
    super (inReplyTo);
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
}
