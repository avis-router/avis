package org.avis.router;

class InvalidSubscriptionException extends RuntimeException
{
  public InvalidSubscriptionException (String message)
  {
    super (message);
  }
}
