package org.avis.net.tools;

import java.util.Queue;

import org.avis.util.IllegalOptionException;

public class EcOptions extends ToolOptions
{
  public String subscription;

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
