package org.avis.util;

import java.util.LinkedList;
import java.util.Queue;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;

import static org.avis.util.Text.shortException;

/**
 * A set of command line options. Subclasses implement
 * {@link #handleArg(Queue)} and {@link #checkOptions()}.
 * 
 * @author Matthew Phillips
 */
public abstract class CommandLineOptions
{
  /**
   * Create a new instance.
   * 
   * @see #handleOptions(String...)
   */
  public CommandLineOptions ()
  {
    // zip
  }

  /**
   * Create a new instance and immediately parse an array of command
   * line options and create an option set.
   * 
   * @param argv The command line options.
   * 
   * @throws IllegalConfigOptionException if an error is detected.
   * 
   * @see #handleOptions(String...)
   */
  public CommandLineOptions (String... argv)
    throws IllegalCommandLineOption
  {
    handleOptions (argv);
  }
  
  protected void handleOptions (String... argv)
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
   * Handle an error in parsing command line options or starting the
   * command line application by printing an error on the console and
   * exiting the VM with an error code.
   * 
   * @param appName The application's name.
   * @param usage The command line usage string. The app name will be
   *                appended to this, so this should just include the
   *                options summary plus any detail.
   * @param ex The error that triggered the exit.
   *                IllegalCommandLineOption is handled specially by
   *                printing a usage string.
   */
  public static void handleError (String appName, String appDesc,
                                  String usage, Exception ex)
  {
    if (ex instanceof IllegalCommandLineOption)
    {
      System.err.println (appName + ": " + ex.getMessage ());
      System.err.println ();
      
      System.err.println (appName + ": " + appDesc);
      System.err.println ();
      
      System.err.println ("Usage:");
      System.err.println ();
      System.err.print ("  ");
      System.err.print (appName);
      System.err.print (' ');
      System.err.println (usage);
      
      System.exit (1);
    } else
    {
      System.err.println (appName + ": error on startup: " + 
                          shortException (ex));
      System.err.println ();
      
      System.exit (2);
    }
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
   * Called at the end of parsing. Throw IllegalOptionException if the
   * command line options are not in a valid state e.g. a required
   * parameter not specified.
   */
  protected void checkOptions ()
    throws IllegalCommandLineOption
  {
    // zip
  }
  
  protected static String bareArg (Queue<String> args)
    throws IllegalCommandLineOption
  {
    if (args.isEmpty ())
      throw new IllegalCommandLineOption ("Missing parameter");
    else
      return args.remove ();
  }
  
  /**
   * Take an option switch plus its string parameter (in the form of
   * {-s string}) off the queue. The string parameter may not begin
   * with "-" (i.e look like an option switch)
   * 
   * @param args The args queue.
   * @return The parameter to the switch.
   * @throws IllegalConfigOptionException if no parameter is present.
   */
  protected static String arg (Queue<String> args)
    throws IllegalCommandLineOption
  {
    String option = args.remove ();
    
    if (args.isEmpty ())
      throw new IllegalCommandLineOption (option, "Missing parameter");
    else
      return args.remove ();
  }
  
  /**
   * Take an option switch plus its string parameter (in the form of
   * {-s string}) off the queue. This is the same as arg (), but the
   * string parameter may not begin with "-" (i.e look like an option
   * switch)
   * 
   * @param args The args queue.
   * @return The parameter to the switch.
   * @throws IllegalConfigOptionException if no parameter is present.
   */
  protected static String stringArg (Queue<String> args)
    throws IllegalCommandLineOption
  {
    String option = args.peek ();
    String arg = arg (args);
    
    if (arg.startsWith ("-"))
      throw new IllegalCommandLineOption 
        (option, "Missing parameter: was followed by option" + arg);
    
    return arg;
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
