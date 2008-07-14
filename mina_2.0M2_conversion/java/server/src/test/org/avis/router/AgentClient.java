package org.avis.router;

import java.util.Timer;

import org.apache.mina.common.IoSession;

public abstract class AgentClient extends SimpleClient
{
  public volatile int sentMessageCount;
  public volatile int receivedMessageCount;
  
  protected Timer timer;

  public AgentClient (String name, String host, int port)
    throws Exception
  {
    super (name, host, port);
    
    timer = new Timer ();

    sentMessageCount = 0;
    receivedMessageCount = 0;

    connect ();
  }
  
  public String report ()
  {
    return clientName +
           " sent " + sentMessageCount + 
           ", received " + receivedMessageCount;
  }
  
  @Override
  public void messageReceived (IoSession session, Object message)
    throws Exception
  {
    receivedMessageCount++;

    super.messageReceived (session, message);
  }
}
