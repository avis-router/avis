package org.avis.net.tools;

import java.util.Queue;

import org.avis.util.IllegalOptionException;

public class EpOptions extends ToolOptions
{
  public boolean deliverInsecure;

  public EpOptions ()
  {
    deliverInsecure = true;
  }
  
  @Override
  protected void handleArg (Queue<String> args)
    throws IllegalOptionException
  {
    if (takeArg (args, "-x"))
      deliverInsecure = false;
    else if (takeArg (args, "-X"))
      deliverInsecure = true;
    else
      super.handleArg (args);
  }
}
