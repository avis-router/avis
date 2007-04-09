package org.avis.net.tools;

import java.util.Queue;

import java.net.URISyntaxException;

import org.avis.net.common.ElvinURI;
import org.avis.util.CommandLineOptions;
import org.avis.util.IllegalOptionException;

public class ToolOptions extends CommandLineOptions
{   
  public ElvinURI elvinUri;
  
  @Override
  protected void handleArg (Queue<String> args)
    throws IllegalOptionException
  {
    String arg = args.peek ();
    
    if (arg.equals ("-e"))
    {
      try
      {
        elvinUri = new ElvinURI (stringArg (args));
      } catch (URISyntaxException ex)
      {
        throw new IllegalOptionException
          (arg, "Error in Elvin URI: " + ex.getMessage ());
      }
    }
  }
  
  @Override
  protected void checkOptions ()
  {
    if (elvinUri == null)
      throw new IllegalOptionException ("-e", "Missing Elvin URI");
  }
}
