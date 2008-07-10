package org.avis.tools;

import java.util.Queue;

import org.avis.util.IllegalCommandLineOption;

/**
 * Options for the ec command.
 * 
 * @author Matthew Phillips
 */
public class EcOptions extends ToolOptions
{
  public static final String DESCRIPTION = 
    "Subscribe to notifications from an Elvin router";
  
  public static final String USAGE =
    COMMON_USAGE_SUMMARY + " subscription\n\n" + COMMON_USAGE_DETAIL;

  /**
   * The subscription expression.
   */
  public String subscription;

  public EcOptions (String... args)
  {
    super (args);
  }
  
  @Override
  protected void handleArg (Queue<String> args)
    throws IllegalCommandLineOption
  {
    String arg = args.peek ();
    
    if (!arg.startsWith ("-"))
    {
      if (subscription == null)
        subscription = args.remove ();
      else
        throw new IllegalCommandLineOption ("Can only have one subscription");
    } else
    {
      super.handleArg (args);
    }
  }
  
  @Override
  protected void checkOptions ()
  {
    super.checkOptions ();

    if (subscription == null)
      throw new IllegalCommandLineOption ("No subscription specified");
  }
}
