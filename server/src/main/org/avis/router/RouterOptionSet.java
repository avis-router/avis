package org.avis.router;

import org.avis.config.OptionSet;

import static org.avis.common.Common.DEFAULT_PORT;

public class RouterOptionSet extends OptionSet
{
  public RouterOptionSet ()
  {
    add ("Port", 1, DEFAULT_PORT, 65535);
    add ("Listen", "elvin://0.0.0.0");
    
    inheritFrom (ConnectionOptionSet.CONNECTION_OPTION_SET);
  }
}