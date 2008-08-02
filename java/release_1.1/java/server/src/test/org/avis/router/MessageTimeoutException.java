package org.avis.router;

public class MessageTimeoutException extends Exception
{
  public MessageTimeoutException (String message)
  {
    super (message);
  }

  public MessageTimeoutException (Throwable cause)
  {
    super (cause);
  }

  public MessageTimeoutException (String message, Throwable cause)
  {
    super (message, cause);
  }

}
