package org.avis.io.messages;


/**
 * Synthetic message generated when a request timeout has elapsed.
 * 
 * @author Matthew Phillips
 */
public class RequestTimeoutMessage extends SyntheticMessage
{
  public static final int ID = -2;
  
  /**
   * The request that timed out.
   */
  public final RequestMessage<?> request;

  public RequestTimeoutMessage (RequestMessage<?> request)
  {
    this.request = request;
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
}
