package org.avis.common;

import java.net.URISyntaxException;

/**
 * Unchecked invalid URI exception. This is used in places that it
 * would be irritating to use the checked Java {@link URISyntaxException}.
 * 
 * @author Matthew Phillips
 */
public class InvalidURIException extends RuntimeException
{
  public InvalidURIException (String uri, String message)
  {
    super ("Invalid URI \"" + uri + "\": " + message);
  }
}
