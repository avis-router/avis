package org.avis.io;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.junit.Test;

import static java.lang.Thread.sleep;

import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.LivenessFailureMessage;
import org.avis.io.messages.RequestTimeoutMessage;
import org.avis.io.messages.TestConn;
import org.avis.logging.Log;

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
    
    RequestTrackingFilter requestTrackingFilter = 
      new RequestTrackingFilter (2, Integer.MAX_VALUE);
    
    testSetup.connectorConfig.getFilterChain ().addLast 
      ("tracker", requestTrackingFilter);
    
    TestingIoHandler connectListener = new TestingIoHandler ();
    
    testSetup.connect (new NullAcceptorListener (), connectListener);
    
    // send message, wait for timeout message
    connectListener.message = null;
    
    ConnRqst fedConnRqst = new ConnRqst (1, 0);
    
    testSetup.session.write (fedConnRqst);
    
    connectListener.waitForMessage ();

    assertNotNull (connectListener.message);
    assertEquals (RequestTimeoutMessage.ID, connectListener.message.typeId ());
    assertSame (fedConnRqst, ((RequestTimeoutMessage)connectListener.message).request);
    
    // try again, with two in the pipe
    connectListener.message = null;
    
    fedConnRqst = new ConnRqst (1, 0);
    
    testSetup.session.write (fedConnRqst);
    
    Thread.sleep (1000);
    
    testSetup.session.write (new ConnRqst (1, 0));
    
    connectListener.waitForMessage ();

    assertNotNull (connectListener.message);
    assertSame (fedConnRqst, ((RequestTimeoutMessage)connectListener.message).request);
    
    testSetup.close ();
    
    assertTrue (requestTrackingFilter.sharedResourcesDisposed ());
  }
  
  /**
   * Send a message check that RequestTracker generates a
   * TestConn message and sends LivenessTimeout when no reponse.
   */
  @Test
  public void livenessTimeout () 
    throws Exception
  {
    Log.enableLogging (Log.TRACE, true);
    Log.enableLogging (Log.DIAGNOSTIC, true);
    
    AcceptorConnectorSetup testSetup = new AcceptorConnectorSetup ();
    
    RequestTrackingFilter requestTrackingFilter = 
      new RequestTrackingFilter (1, 1);
    
    testSetup.connectorConfig.getFilterChain ().addLast 
      ("tracker", requestTrackingFilter);
    
    TestingIoHandler acceptorListener = new TestingIoHandler ();
    TestingIoHandler connectListener = new TestingIoHandler ();
    
    testSetup.connect (acceptorListener, connectListener);
    
    // wait for TestConn message
    acceptorListener.waitForMessage ();

    assertEquals (TestConn.ID, acceptorListener.message.typeId ());
    
    sleep (1000);
    
    // wait for LivenessTimeout
    connectListener.waitForMessage ();
    assertEquals (LivenessFailureMessage.ID, connectListener.message.typeId ());
    
    testSetup.close ();
    
    assertTrue (requestTrackingFilter.sharedResourcesDisposed ());
  }
  
  static class NullAcceptorListener 
    extends IoHandlerAdapter implements IoHandler
  {
    // zip
  }
}
