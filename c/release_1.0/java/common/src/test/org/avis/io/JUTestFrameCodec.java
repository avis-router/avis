package org.avis.io;

import java.util.HashMap;
import java.util.Random;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.NotifyDeliver;
import org.avis.util.AutoClose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for FrameCodec.
 * 
 * @author Matthew Phillips
 */
public class JUTestFrameCodec
{
  private AutoClose autoClose = new AutoClose ();

  @After
  public void cleanup ()
  {
    autoClose.close ();
  }
  
  /**
   * TODO: fails (takes ~60 seconds) under MINA 2.0M2. re-enable when fixed
   * See https://issues.apache.org/jira/browse/DIRMINA-609.
   */
  @Test
  @Ignore
  public void bigFrames ()
    throws Exception
  {
    TestingIoHandler acceptorListener = new TestingIoHandler ();
    
    AcceptorConnectorSetup testSetup = new AcceptorConnectorSetup ();
    
    testSetup.connect (acceptorListener, new TestingIoHandler ());
    
    autoClose.add (testSetup);
    
    HashMap<String, Object> attributes = new HashMap<String, Object> ();
    
    attributes.put ("string", bigString ());
    attributes.put ("blob", new byte [1024 * 1024]);
    
    NotifyDeliver notifyDeliver = 
      new NotifyDeliver (attributes, new long [] {1}, new long [0]);
       
    testSetup.connectorSession.write (notifyDeliver);
   
    Message message = acceptorListener.waitForMessage ();
    
    if (message instanceof ErrorMessage)
    {
      fail ("Error message received: " + 
            ((ErrorMessage)message).formattedMessage ());
    }
    
    NotifyDeliver notify = (NotifyDeliver)acceptorListener.waitForMessage ();
    
    assertEquals (attributes.get ("string"), notify.attributes.get ("string"));
    assertEquals (((byte [])attributes.get ("blob")).length, 
                  ((byte [])notify.attributes.get ("blob")).length);
    
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
