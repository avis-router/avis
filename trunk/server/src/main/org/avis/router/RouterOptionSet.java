package org.avis.router;

import org.avis.config.OptionSet;

import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.router.ConnectionOptionSet.CONNECTION_OPTION_SET;

public class RouterOptionSet extends OptionSet
{
  public RouterOptionSet ()
  {
    add ("Port", 1, DEFAULT_PORT, 65535);
    add ("Listen", "elvin://0.0.0.0");
    
    inheritFrom (CONNECTION_OPTION_SET);
  }
}