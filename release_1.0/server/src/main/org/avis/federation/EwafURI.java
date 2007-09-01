package org.avis.federation;

import org.avis.common.ElvinURI;
import org.avis.common.InvalidURIException;

import static org.avis.federation.Federation.DEFAULT_EWAF_PORT;
import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;

/**
 * A URI specifying an Elvin wide-area federation endpoint.
 * 
 * @author Matthew Phillips
 */
public class EwafURI extends ElvinURI
{
  public EwafURI (String uri)
    throws InvalidURIException
  {
    super (uri);
  }
  
  @Override
  protected boolean validScheme (String schemeToCheck)
  {
    return schemeToCheck.equals ("ewaf");
  }
  
  @Override
  protected void init ()
  {
    super.init ();
    
    this.versionMajor = VERSION_MAJOR;
    this.versionMinor = VERSION_MINOR;
    this.port = DEFAULT_EWAF_PORT;
  }
}
