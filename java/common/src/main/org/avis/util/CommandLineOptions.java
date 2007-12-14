package org.avis.util;

import java.util.LinkedList;
import java.util.Queue;

import org.avis.logging.Log;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;

/**
 * A set of command line options. Subclasses implement
 * {@link #handleArg(Queue)} and {@link #checkOptions()}.
 * 
 * @author Matthew Phillips
 */
public abstract class CommandLineOptions
{
  /**
   * Parse an array of command line options.
   * 
   * @param argv The command line options.
   * 
   * @throws IllegalConfigOptionException if an error is detected.
   */
  public void parse (String [] argv)
    throws IllegalCommandLineOption
  {
    Queue<String> args = new LinkedList<String> (asList (argv));
    
    while (!args.isEmpty ())
    {
      if (!argHandled (args))
      {
        String arg = args.peek ();
        
        if (arg.startsWith ("-"))
          throw new IllegalCommandLineOption (arg, "Unknown option");
        else
          throw new IllegalCommandLineOption ("Unknown extra parameter: " + arg);
      }
    }
    
    checkOptions ();
  }
  
  /**
   * Parse the command line options successfully or exit() with a usage
   * error.
   * 
   * @see #usageError(String)
   * @see #usage()
   */
  public void parseOrExit (String [] args)
  {
    try
    {
      parse (args);
    } catch (IllegalCommandLineOption ex)
    {
      usageError (ex.getMessage ());
    }
  }
  
  protected void usageError (String message)
  {
    String applicationName = Log.applicationName ();
    
    if (applicationName != null)
      System.err.print (applicationName + ": ");
    
    System.err.println (message);
    System.err.println ();
    System.err.println (usage ());
    System.err.println ();
    System.exit (1);
  }

  private boolean argHandled (Queue<String> args)
  {
    int size = args.size ();
    
    handleArg (args);
    
    return size != args.size ();
  }
  
  /**
   * If an argument is found at the head of the queue that can be
   * handled, handle it and remove (plus any parameters), otherwise do
   * nothing.
   * 
   * @param args The commnd line queue.
   * 
   * @throws IllegalConfigOptionException
   */
  protected abstract void handleArg (Queue<String> args)
    throws IllegalCommandLineOption;

  /**
   * Generate a usage string suitable for printing to the console.
   */
  protected abstract String usage ();

  /**
   * Called at the end of parsing. Throw IllegalOptionException if the
   * command line options are not in a valid state e.g. a required
   * parameter not specified.
   */
  protected void checkOptions ()
    throws IllegalCommandLineOption
  {
    // zip
  }
  
  /**
   * Take an option switch plus its string parameter (in the form of
   * {-s string}) off the queue.
   * 
   * @param args The args queue.
   * @return The parameter to the switch.
   * @throws IllegalConfigOptionException if no parameter is present.
   */
  protected static String stringArg (Queue<String> args)
    throws IllegalCommandLineOption
  {
    String option = args.remove ();
    
    if (args.isEmpty ())
      throw new IllegalCommandLineOption (option, "Missing parameter");
    else
      return args.remove ();
  }

  /**
   * Take an option switch plus its integer parameter (in the form of
   * {-i integer}) off the queue.
   * 
   * @param args The args queue.
   * @return The parameter to the argument.
   * @throws IllegalConfigOptionException if no parameter is present or it
   *           is not a number.
   */
  protected static int intArg (Queue<String> args)
  {
    String arg = args.peek ();
    String value = stringArg (args);
  
    try
    {
      return parseInt (value);
    } catch (NumberFormatException ex)
    {
      throw new IllegalCommandLineOption (arg, "Not a valid number: " + value);
    }
  }

  /**
   * Take an argument off the queue and return true if it matches the
   * given argument.
   */
  protected static boolean takeArg (Queue<String> args, String arg)
  {
    if (args.peek ().equals (arg))
    {
      args.remove ();
      
      return true;
    } else
    {
      return false;
    }
  }
}
