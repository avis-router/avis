package org.avis.net.server;

import java.util.Date;
import java.util.TimerTask;

import org.avis.Notification;

import static dsto.dfc.logging.Log.alarm;

public class GoodClient extends AgentClient
{
  public GoodClient (String name, String host, int port)
    throws Exception
  {
    super (name, host, port);

    subscribe ("Channel == 'goodclient'");
  }
  
  public void startSending ()
  {
    timer.schedule (new SendTask (), 0, 20);
  }

  public void stopSending ()
  {
    timer.cancel ();
  }
  
  class SendTask extends TimerTask
  {
    @Override
    public void run ()
    {
      try
      {
        Notification ntfn = new Notification ();
        
        ntfn.put ("From", getClass ().getName ());
        ntfn.put ("Time", new Date ().toString ());
        ntfn.put ("Channel", "goodclient");
        
        sendNotify (ntfn);
        
        sentMessageCount++;
      } catch (Exception ex)
      {
        alarm ("Failed to send notification", this, ex);
      }
    }
  }
}
