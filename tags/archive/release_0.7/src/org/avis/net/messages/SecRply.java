package org.avis.net.messages;

public class SecRply extends XidMessage
{
  public static final int ID = 55;
  
  public SecRply ()
  {
    // zip
  }
  
  public SecRply (SecRqst inReplyTo)
  {
    super (inReplyTo);
  }

  @Override
  public int typeId ()
  {
    return ID;
  }  
}
