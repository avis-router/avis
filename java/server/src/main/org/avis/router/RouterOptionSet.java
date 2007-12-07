package org.avis.router;

import org.avis.config.OptionSet;
import org.avis.config.OptionTypeURI;

import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.io.Net.uri;
import static org.avis.router.ConnectionOptionSet.CONNECTION_OPTION_SET;

public class RouterOptionSet extends OptionSet
{
  public RouterOptionSet ()
  {
    add ("Port", 1, DEFAULT_PORT, 65535);
    add ("Listen", "elvin://0.0.0.0");
    add ("Client.Idle-Connection-Timeout", 1, 15, Integer.MAX_VALUE);
    add ("IO.Use-Direct-Buffers", true);
    add ("TLS.Keystore", new OptionTypeURI (), uri ("router.ks"));
    add ("TLS.Keystore-Passphrase", "");
    
    inheritFrom (CONNECTION_OPTION_SET);
  }
}