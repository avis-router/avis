package org.avis.federation;

import org.avis.util.OptionSet;
import org.avis.util.Options;

public class FederationOptions extends Options
{
  public static OptionSet OPTION_SET = new FederationOptionSet ();
  
  static class FederationOptionSet extends OptionSet
  {
    public FederationOptionSet ()
    {
      add ("Federation.Connection-Timeout", 1, 20, Integer.MAX_VALUE);
    }
  }
  
  public FederationOptions ()
  {
    super (OPTION_SET);
  }
}
