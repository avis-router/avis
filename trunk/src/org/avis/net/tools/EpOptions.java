package org.avis.net.tools;

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
