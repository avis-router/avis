package org.avis.common;

/**
 * Like InterruptedException, but less annoying. Classes that use
 * Thread.wait () etc and want to avoid everyhing up the chain
 * declaring InterruptedException, should catch it and re-throw this.
 * 
 * @author Matthew Phillips
 */
public class RuntimeInterruptedException extends RuntimeException
{
  public RuntimeInterruptedException (InterruptedException cause)
  {
    super (cause);
  }
}
