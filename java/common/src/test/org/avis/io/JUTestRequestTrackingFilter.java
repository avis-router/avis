package org.avis.io;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
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
  public void requestTimeout () 
    throws Exception
  {
    AcceptorConnectorSetup testSetup = new AcceptorConnectorSetup ();
    
    RequestTrackingFilter filter = new RequestTrackingFilter (2000);
    
    testSetup.connector.getFilterChain ().addLast ("tracker", filter);
    
    TestingIoHandler connectListener = new TestingIoHandler ();
    
    testSetup.connect (new NullAcceptorListener (), connectListener);
    
    // send message, wait for timeout message
    connectListener.message = null;
    
    ConnRqst fedConnRqst = new ConnRqst (1, 0);
    
    testSetup.connectorSession.write (fedConnRqst);
    
    connectListener.waitForMessage ();

    assertNotNull (connectListener.message);
    assertEquals (RequestTimeoutMessage.ID, connectListener.message.typeId ());
    assertSame (fedConnRqst, ((RequestTimeoutMessage)connectListener.message).request);
    
    // try again, with two in the pipe
    connectListener.message = null;
    
    fedConnRqst = new ConnRqst (1, 0);
    
    testSetup.connectorSession.write (fedConnRqst);
    
    Thread.sleep (1000);
    
    testSetup.connectorSession.write (new ConnRqst (1, 0));
    
    connectListener.waitForMessage ();

    assertNotNull (connectListener.message);
    assertSame (fedConnRqst, ((RequestTimeoutMessage)connectListener.message).request);
    
    testSetup.close ();
    
    assertTrue (SharedExecutor.sharedExecutorDisposed ());
  }
  
  static class NullAcceptorListener 
    extends IoHandlerAdapter implements IoHandler
  {
    // zip
  }
}
