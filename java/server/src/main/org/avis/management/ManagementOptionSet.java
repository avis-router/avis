package org.avis.management;

import org.avis.config.OptionSet;
import org.avis.config.OptionTypeHttpUrl;

import static org.avis.io.Net.url;

/**
 * Configuration option set for Avis web management.
 * 
 * @author Matthew Phillips
 */
public class ManagementOptionSet extends OptionSet
{
  public static final int DEFAULT_PORT = 8017;

  public static final OptionSet OPTION_SET = new ManagementOptionSet ();
  
  protected ManagementOptionSet ()
  {
    add ("Management.Activated", false);
    add ("Management.Listen",
         new OptionTypeHttpUrl (), url ("http://0.0.0.0:" + DEFAULT_PORT));
    add ("Management.Admin-Name", "admin");
    add ("Management.Admin-Password", "");
  }
}