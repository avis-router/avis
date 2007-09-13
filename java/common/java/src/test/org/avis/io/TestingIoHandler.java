package org.avis.io;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

import org.avis.io.messages.Message;

import static org.junit.Assert.fail;

/**
 * IO handler that allows a test client to wait for an incoming
 * message.
 * 
 * @author Matthew Phillips
 */
public class TestingIoHandler
  extends IoHandlerAdapter implements IoHandler
{
  public Message message;

  @Override
  public synchronized void messageReceived (IoSession session, 
                                            Object theMessage)
    throws Exception
  {
    message = (Message)theMessage;
    
    notifyAll ();
  }

  public synchronized Message waitForMessage ()
    throws InterruptedException
  {
    if (message == null)
      wait (5000);
    
    if (message == null)
      fail ("No message received");
    
    return message;
  }
}