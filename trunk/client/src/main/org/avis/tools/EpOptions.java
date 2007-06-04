package org.avis.tools;

/**
 * The ep command line options.
 * 
 * @author Matthew Phillips
 */
public class EpOptions extends ToolOptions
{
  private static final String USAGE =
    "Usage: ep " + COMMON_USAGE_SUMMARY + "\n\n" +
    COMMON_USAGE_DETAIL;
  
  @Override
  protected String usage ()
  {
    return USAGE;
  }
}
