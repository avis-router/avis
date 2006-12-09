package org.avis.util;

public class IllegalOptionException extends IllegalArgumentException
{
  public IllegalOptionException (String message)
  {
    super (message);
  }

  public IllegalOptionException (String option, String message)
  {
    this (message + ": " + option);
  }
}
