package org.avis.client.examples;

import java.util.Queue;

import org.avis.common.ElvinURI;
import org.avis.common.InvalidURIException;
import org.avis.util.CommandLineOptions;
import org.avis.util.IllegalOptionException;

import static java.lang.System.getProperty;

/**
 * Command line options that are common across the example
 * applications.
 * 
 * @author Matthew Phillips
 */
public class ExampleOptions extends CommandLineOptions
{
  protected static final String USAGE =
    "-e elvin\n\n" +
    "  -e elvin  Set the Elvin URI e.g. elvin://host:port";
  
  /**
   * The Elvin router to connect to.
   */
  public ElvinURI elvinUri;

  private String appName;
  
  ExampleOptions (String appName)
  {
    this.elvinUri = new ElvinURI (getProperty ("elvin", "elvin://localhost"));
    
    this.appName = appName;
  }
  
  @Override
  protected String usage ()
  {
    return "usage: " + appName + " " + USAGE;
  }
  
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
      } catch (InvalidURIException ex)
      {
        throw new IllegalOptionException
          (arg, "Invalid Elvin URI: " + ex.getMessage ());
      }
    }
  }
}
