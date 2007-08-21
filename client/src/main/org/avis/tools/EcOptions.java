package org.avis.tools;

import java.util.Queue;

import org.avis.util.IllegalOptionException;

/**
 * Options for the ec command.
 * 
 * @author Matthew Phillips
 */
public class EcOptions extends ToolOptions
{
  private static final String USAGE =
    "Usage: ec " + COMMON_USAGE_SUMMARY + " subscription\n\n" +
    COMMON_USAGE_DETAIL;

  /**
   * The subscription expression.
   */
  public String subscription;
 
  @Override
  protected String usage ()
  {
    return USAGE;
  }
  
  @Override
  protected void handleArg (Queue<String> args)
    throws IllegalOptionException
  {
    String arg = args.peek ();
    
    if (!arg.startsWith ("-"))
    {
      if (subscription == null)
        subscription = args.remove ();
      else
        throw new IllegalOptionException ("Can only have one subscription");
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
      throw new IllegalOptionException ("No subscription specified");
  }
}
