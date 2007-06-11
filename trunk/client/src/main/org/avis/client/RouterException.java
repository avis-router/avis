package org.avis.client;

import java.io.IOException;

import org.avis.io.messages.Nack;

/**
 * An exception indicating the Elvin router rejected one of the
 * client's requests.
 * 
 * @author Matthew Phillips
 */
public class RouterException extends IOException
{
  RouterException (Nack nack)
  {
    super (nack.formattedMessage ());
  }
}
