package org.avis.net.server;

class InvalidSubscriptionException extends RuntimeException
{
  public InvalidSubscriptionException (String message)
  {
    super (message);
  }
}
