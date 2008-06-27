package org.avis.server;

class InvalidSubscriptionException extends RuntimeException
{
  public InvalidSubscriptionException (String message)
  {
    super (message);
  }
}
