package org.avis.federation;

import org.avis.common.ElvinURI;

public class EwafURI extends ElvinURI
{
  public static final int VERSION_MAJOR = 1;
  public static final int VERSION_MINOR = 0;
  
  public static final int DEFAULT_EWAF_PORT = 2916;

  public EwafURI (String uri)
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
