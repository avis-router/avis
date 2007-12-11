package org.avis.io;

import java.io.IOException;

/**
 * Throws by the frame codec when a frame is too big to be decoded.
 * 
 * @author Matthew Phillips
 */
public class FrameTooLargeException extends IOException
{
  public FrameTooLargeException (int maxLength, int actualLength)
  {
    super ("Frame size of " + actualLength + 
           " bytes is larger than maximum " + maxLength);
  }
}
