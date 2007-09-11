package org.avis.net.messages;

public class DisconnRqst extends XidMessage
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
}
