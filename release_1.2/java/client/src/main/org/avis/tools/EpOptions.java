package org.avis.tools;

/**
 * The ep command line options.
 * 
 * @author Matthew Phillips
 */
public class EpOptions extends ToolOptions
{
  public static final String USAGE =
    COMMON_USAGE_SUMMARY + "\n\n" + COMMON_USAGE_DETAIL;
  
  public EpOptions (String... args)
  {
    super (args);
  }
}