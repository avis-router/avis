package org.avis.io;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.junit.Test;

import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.RequestTimeoutMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JUTestRequestTrackingFilter
{
  /**
   * Send a message with reply, check that RequestTracker generates a
   * timeout message.
   */
  @Test
  public void timeout () 
    throws Exception
  {
    AcceptorConnectorSetup testSetup = new AcceptorConnectorSetup ();
    
    RequestTrackingFilter requestTrackingFilter = new RequestTrackingFilter (2);
    
    testSetup.connectorConfig.getFilterChain ().addLast 
      ("tracker", requestTrackingFilter);
    
    TestingIoHandler connectListener = new TestingIoHandler ();
    
    testSetup.connect (new AcceptorListener (), connectListener);
    
    // send message, wait for timeout message
    connectListener.message = null;
    
    ConnRqst fedConnRqst = new ConnRqst (1, 0);
    
    testSetup.session.write (fedConnRqst);
    
    synchronized (connectListener)
    {
      connectListener.wait (3000);
    }

    assertNotNull (connectListener.message);
    assertEquals (RequestTimeoutMessage.ID, connectListener.message.typeId ());
    assertSame (fedConnRqst, ((RequestTimeoutMessage)connectListener.message).request);
    
    // try again, with two in the pipe
    connectListener.message = null;
    
    fedConnRqst = new ConnRqst (1, 0);
    
    testSetup.session.write (fedConnRqst);
    
    Thread.sleep (1000);
    
    testSetup.session.write (new ConnRqst (1, 0));
    
    synchronized (connectListener)
    {
      connectListener.wait (3000);
    }

    assertNotNull (connectListener.message);
    assertSame (fedConnRqst, ((RequestTimeoutMessage)connectListener.message).request);
    
    testSetup.close ();
    
    assertTrue (requestTrackingFilter.sharedResourcesDisposed ());
  }
  
  static class AcceptorListener extends IoHandlerAdapter implements IoHandler
  {
    // zip
  }
}
