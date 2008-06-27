package org.avis.client;

import java.io.IOException;

/**
 * Thrown when an operation that requires a connection to the router
 * is attempted on a closed connection.
 * 
 * @author Matthew Phillips
 */
public class NotConnectedException extends IOException
{
  public NotConnectedException (String message)
  {
    super (message);
  }
}
