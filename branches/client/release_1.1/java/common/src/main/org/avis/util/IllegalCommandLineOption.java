package org.avis.util;

public class IllegalCommandLineOption extends IllegalArgumentException
{
  public IllegalCommandLineOption (String message)
  {
    super (message);
  }

  public IllegalCommandLineOption (String option, String message)
  {
    this ("Error in command line option \"" + option + "\": " + message);
  }
}
