package org.avis.io;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;

import org.avis.io.messages.LivenessFailureMessage;
import org.avis.io.messages.TestConn;
import org.avis.logging.Log;

import org.junit.Test;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JUTestLivenessFilter
{
  @Test
  public void livenessTimeout () 
    throws Exception
  {
    Log.enableLogging (Log.TRACE, true);
    Log.enableLogging (Log.DIAGNOSTIC, true);
    
    AcceptorConnectorSetup testSetup = new AcceptorConnectorSetup ();
    
    LivenessFilter filter = new LivenessFilter (1000, 1000);
    
    testSetup.connectorConfig.getFilterChain ().addLast 
      ("liveness", filter);
    
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
    
    LivenessFilter.Tracker tracker = 
      LivenessFilter.trackerFor (testSetup.session);
    
    testSetup.close ();
    
    assertTrue (SharedExecutor.sharedExecutorDisposed ());
    assertTrue (tracker.isDisposed ());
  }
  
  static class NullAcceptorListener 
    extends IoHandlerAdapter implements IoHandler
  {
    // zip
  }
}
