package org.avis.util;

/**
 * General command line utilities.
 * 
 * @author Matthew Phillips
 */
public final class CommandLine
{
  private CommandLine ()
  {
    // cannot instantiate
  }
  
  /**
   * Get a string argument from a given index, throwing a descriptive
   * exception if the argument is missing.
   * 
   * @param args The command line arguments.
   * @param arg The argument index to retrieve.
   * @return The argument at arg.
   * 
   * @throws IllegalArgumentException if arg does not exist.
   */
  public static String stringArg (String [] args, int arg)
  {
    try
    {
      return args [arg];
    } catch (ArrayIndexOutOfBoundsException ex)
    {
      throw new IllegalArgumentException
        ("Missing parameter for \"" + args [arg - 1] + "\" option");
    }
  }

  /**
   * Get an integer argument from a given index, throwing a
   * descriptive exception if the argument is missing or not a number.
   * 
   * @param args The command line arguments.
   * @param arg The argument index to retrieve.
   * @return The argument at arg.
   * 
   * @throws IllegalArgumentException if arg does not exist or is not
   *           a number.
   */
  public static int intArg (String [] args, int arg)
  {
    try
    {
      return Integer.parseInt (stringArg (args, arg));
    } catch (NumberFormatException ex)
    {
      throw new IllegalArgumentException
        (args [arg - 1] + ": not a number: " + args [arg]);
    }
  }
}
