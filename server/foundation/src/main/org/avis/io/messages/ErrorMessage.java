package org.avis.io.messages;

import static org.avis.util.Text.shortException;

/**
 * Synthetic message used to signal protocol errors.
 * 
 * @author Matthew Phillips
 */
public class ErrorMessage extends SyntheticMessage
{
  public static final int ID = -1;
  
  public Throwable error;
  public Message cause;
  
  public ErrorMessage (Throwable error, Message cause)
  {
    this.error = error;
    this.cause = cause;
  }

  /**
   * Generate an error message suitable for presentation as a
   * debugging aid.
   */
  public String formattedMessage ()
  {
    StringBuilder message = new StringBuilder ();
    
    if (cause == null)
      message.append ("Error decoding XDR frame");
    else
      message.append ("Error decoding ").append (cause.name ());
    
    if (error != null)
      message.append (": ").append (shortException (error));
    
    return message.toString (); 
  }
  
  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public String toString ()
  {
    return "ErrorMessage: " + formattedMessage ();
  }
}
