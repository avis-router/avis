package org.avis.client;

import java.util.EventObject;

/**
 * A logging event fired by an elvin client.
 * 
 * @see Elvin#addLogListener(ElvinLogListener)
 * 
 * @author Matthew Phillips
 */
public class ElvinLogEvent extends EventObject
{
  public enum Type {DIAGNOSTIC, WARNING, ERROR}
  
  /**
   * The type of message.
   */
  public final Type type;
  
  /**
   * The message text.
   */
  public final String message;
  
  /**
   * The exception that triggered the message, if any (or null).
   */
  public final Throwable exception;

  public ElvinLogEvent (Elvin elvin, Type type, 
                        String message, Throwable exception)
  {
    super (elvin);
    
    this.type = type;
    this.message = message;
    this.exception = exception;
  }
  
  /**
   * The elvin client connection that fired the event. Same as
   * {@link #getSource()}.
   */
  public Elvin elvin ()
  {
    return (Elvin)getSource ();
  }
}
