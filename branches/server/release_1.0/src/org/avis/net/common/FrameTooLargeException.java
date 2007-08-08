package org.avis.net.common;

import java.io.IOException;

public class FrameTooLargeException extends IOException
{
  public FrameTooLargeException (int maxLength, int actualLength)
  {
    super ("Frame size of " + actualLength + 
           " bytes is larger than maximum " + maxLength);
  }
}
