package org.avis.io;

import org.avis.io.messages.Nack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JUTestNack
{
  @Test
  public void formattedMessage ()
  {
    Nack nack = new Nack ();
    
    nack.args = new Object [] {"foo", "bar"};
    nack.message = "There was a %1 in the %2 (%1, %2) %3 %";
    
    String formattedMessage = nack.formattedMessage ();
    
    assertEquals ("There was a foo in the bar (foo, bar) %3 %",
                  formattedMessage);
  }
}
