package org.avis;

/**
 * Common Avis definitions.
 * 
 * @author Matthew Phillips
 */
public final class Common
{
  public static final int K = 1024;
  public static final int MB = 1024 * 1024;
  public static final int MAX = Integer.MAX_VALUE;
  
  public static final int DEFAULT_PORT = 2917;
  
  public static final int CLIENT_VERSION_MAJOR = 4;
  public static final int CLIENT_VERSION_MINOR = 0;
  
  private Common ()
  {
    // cannot be instantiated
  }

  /**
   * Return just the name (minus the package) of a class.
   */
  public static String className (Class type)
  {
    String name = type.getName ();
    
    return name.substring (name.lastIndexOf ('.') + 1);
  }

  /**
   * Generate a short exception message without package name and
   * message (if null).
   */
  public static String shortException (Throwable ex)
  {
    if (ex.getMessage () == null)
      return className (ex.getClass ());
    else
      return className (ex.getClass ()) + ": " + ex.getMessage ();
  } 
}
