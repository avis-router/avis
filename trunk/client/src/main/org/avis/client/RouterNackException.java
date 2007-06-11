package org.avis.client;

import java.io.IOException;

import org.avis.io.messages.Nack;

/**
 * An exception indicating the Elvin router rejected (NACK'd) one of
 * the client's requests.
 * 
 * @author Matthew Phillips
 */
public class RouterNackException extends IOException
{
  RouterNackException (Nack nack)
  {
    super (nack.formattedMessage ());
  }
}
