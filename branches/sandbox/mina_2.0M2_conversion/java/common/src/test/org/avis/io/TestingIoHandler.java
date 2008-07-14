package org.avis.io;

import junit.framework.AssertionFailedError;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import org.avis.io.messages.Message;

import static org.avis.util.Text.className;
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

  @SuppressWarnings("unchecked")
  public synchronized <T extends Message> T waitForMessage (Class<T> type)
    throws InterruptedException
  {
    waitForMessage ();
    
    if (type.isAssignableFrom (message.getClass ()))
      return (T)message;
    else
      throw new AssertionFailedError ("Expected " + className (type) + ", was " + className (message));
  }

  public synchronized void waitForClose (IoSession session)
  {
    if (!session.isConnected ())
      return;
    
    try
    {
      wait (5000);
    } catch (InterruptedException ex)
    {
      throw new Error (ex);
    }
    
    if (session.isConnected () && !session.isClosing ())
      throw new AssertionFailedError ("Session not closed");
  }
  
  @Override
  public synchronized void sessionClosed (IoSession session)
    throws Exception
  {
    System.out.println ("*** closed");
    notifyAll ();
  }
}