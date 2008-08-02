package org.avis.router;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import static org.avis.logging.Log.alarm;

public class MaliciousClient extends AgentClient
{
  public MaliciousClient (int port)
    throws Exception
  {
    super ("malicious", "localhost", port);
  }

  public MaliciousClient (String name, String host, int port)
    throws Exception
  {
    super (name, host, port);
  }

  public void startFlooding ()
  {
    timer.schedule (new FloodTask (), 0, 1);
  }

  public void stopFlooding ()
  {
    timer.cancel ();
  }
  
  class FloodTask extends TimerTask
  {
    private final byte [] BIG_BLOB = new byte [500 * 1024];

    @Override
    public void run ()
    {
      try
      {
        Map<String, Object> ntfn = new HashMap<String, Object> ();
        
        ntfn.put ("From", getClass ().getName ());
        ntfn.put ("Time", new Date ().toString ());
        ntfn.put ("Payload", BIG_BLOB);
        
        sendNotify (ntfn);
        
        sentMessageCount++;
      } catch (Exception ex)
      {
        alarm ("Failed to send notification", this, ex);
      }
    }
  }
}
