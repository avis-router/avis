package org.avis.client;

import java.io.IOException;

import org.avis.io.messages.Nack;
import org.avis.io.messages.XidMessage;

/**
 * An exception indicating the Elvin router rejected (NACK'd) one of
 * the client's requests.
 * 
 * @author Matthew Phillips
 */
public class RouterNackException extends IOException
{
  RouterNackException (String message)
  {
    super (message);
  }
  
  RouterNackException (XidMessage request, Nack nack)
  {
    super ("Router rejected " + request.name () +
           ": " + nack.errorCodeText () +
           ": " + nack.formattedMessage ());
  }
}
