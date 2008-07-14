package org.avis.io;

import java.util.HashMap;
import java.util.Random;

import org.junit.Test;

import org.avis.io.messages.NotifyDeliver;

import static org.junit.Assert.assertEquals;

/**
 * Tests for FrameCodec.
 * 
 * @author Matthew Phillips
 */
public class JUTestFrameCodec
{
  @Test
  public void bigFrames ()
    throws Exception
  {
    AcceptorConnectorSetup testSetup = new AcceptorConnectorSetup ();
    
    TestingIoHandler acceptorListener = new TestingIoHandler ();
    
    testSetup.connect (acceptorListener, new TestingIoHandler ());
    
    HashMap<String, Object> attributes = new HashMap<String, Object> ();
    
    attributes.put ("string", bigString ());
    attributes.put ("blob", new byte [1024 * 1024]);
    
    NotifyDeliver notifyDeliver = 
      new NotifyDeliver (attributes, new long [] {1}, new long [0]);
       
    testSetup.session.write (notifyDeliver);
   
    NotifyDeliver message = (NotifyDeliver)acceptorListener.waitForMessage ();
    
    assertEquals (attributes.get ("string"), message.attributes.get ("string"));
    assertEquals (((byte [])attributes.get ("blob")).length, 
                  ((byte [])message.attributes.get ("blob")).length);
    
    testSetup.close ();
  }

  private static String bigString ()
  {
    StringBuilder str = new StringBuilder ();
    Random random = new Random (42);
    
    for (int i = 0; i < 800 * 1024; i++)
      str.append ((char)random.nextInt (Character.MAX_CODE_POINT) + 1);
    
    return str.toString ();
  }
}
