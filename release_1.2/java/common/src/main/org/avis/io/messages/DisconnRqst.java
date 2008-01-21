package org.avis.io.messages;

public class DisconnRqst extends RequestMessage<DisconnRply>
{
  public static final int ID = 51;
  
  public DisconnRqst ()
  {
    super (nextXid ());
  }
  
  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public Class<DisconnRply> replyType ()
  {
    return DisconnRply.class;
  }
}
