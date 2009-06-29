package org.avis.management.web;

import java.net.URL;

import org.avis.config.OptionSet;
import org.avis.config.OptionTypeSet;

import static java.util.Collections.singleton;

import static org.avis.io.Net.url;

/**
 * Configuration option set for Avis web management.
 * 
 * @author Matthew Phillips
 */
public class WebManagementOptionSet extends OptionSet
{
  public static final int DEFAULT_PORT = 8017;

  public static final OptionSet OPTION_SET = new WebManagementOptionSet ();
  
  protected WebManagementOptionSet ()
  {
    add ("WebManagement.Activated", false);
    add ("WebManagement.Listen",
         new OptionTypeSet (URL.class), 
         singleton (url ("http://0.0.0.0:" + DEFAULT_PORT)));
  }
}