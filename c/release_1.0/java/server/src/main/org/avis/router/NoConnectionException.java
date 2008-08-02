package org.avis.router;

import org.apache.mina.filter.codec.ProtocolCodecException;

public class NoConnectionException extends ProtocolCodecException
{
  public NoConnectionException (String message)
  {
    super (message);
  }
}
