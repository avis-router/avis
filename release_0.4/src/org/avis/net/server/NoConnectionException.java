package org.avis.net.server;

import org.apache.mina.protocol.ProtocolViolationException;

public class NoConnectionException extends ProtocolViolationException
{
  public NoConnectionException (String message)
  {
    super (message);
  }
}
