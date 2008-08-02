package org.avis.io.messages;

/**
 * Synthetic message sent when a liveness check fails.
 * 
 * @author Matthew Phillips
 */
public class LivenessFailureMessage extends SyntheticMessage
{
  public static final int ID = -3;
  
  @Override
  public int typeId ()
  {
    return ID;
  }
}
