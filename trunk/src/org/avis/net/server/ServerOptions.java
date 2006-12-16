package org.avis.net.server;

import org.avis.util.OptionSet;
import org.avis.util.Options;

import static org.avis.net.ConnectionOptionSet.CONNECTION_OPTION_SET;
import static org.avis.net.server.Server.DEFAULT_PORT;

/**
 * Options used to configure the Avis server.
 * 
 * @author Matthew Phillips
 */
public class ServerOptions extends Options
{
  private static final OptionSet SERVER_OPTIONS = new ServerOptionSet ();

  static class ServerOptionSet extends OptionSet
  {
    public ServerOptionSet ()
    {
      add ("Port", 1, DEFAULT_PORT, 65535);
    }
  }
  
  public ServerOptions ()
  {
    super (SERVER_OPTIONS);
    
    optionSet.inheritFrom (CONNECTION_OPTION_SET);
    
    optionSet.add ("Port", 1, Server.DEFAULT_PORT, 65535);
  }

  public ServerOptions (int port)
  {
    this ();
    
    set ("Port", port);
  }
}