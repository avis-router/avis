package org.avis.net.server;

import org.avis.util.OptionSet;
import org.avis.util.Options;

import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.net.ConnectionOptionSet.CONNECTION_OPTION_SET;

/**
 * Options used to configure the Avis server.
 * 
 * @author Matthew Phillips
 */
public class ServerOptions extends Options
{
  private static final OptionSet OPTION_SET = new ServerOptionSet ();

  static class ServerOptionSet extends OptionSet
  {
    public ServerOptionSet ()
    {
      add ("Port", 1, DEFAULT_PORT, 65535);
    }
  }
  
  public ServerOptions ()
  {
    super (OPTION_SET);
    
    // allow default connection options to be specified also
    optionSet.inheritFrom (CONNECTION_OPTION_SET);
  }

  public ServerOptions (int port)
  {
    this ();
    
    set ("Port", port);
  }
}